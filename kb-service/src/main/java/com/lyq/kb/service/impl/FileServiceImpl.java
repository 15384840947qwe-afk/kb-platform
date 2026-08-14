package com.lyq.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lyq.kb.common.AuthUtil;
import com.lyq.kb.common.UserContext;
import com.lyq.kb.entity.FileRecord;
import com.lyq.kb.mapper.FileMapper;
import com.lyq.kb.rabbit.FileEventProducer;
import com.lyq.kb.service.FileService;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileMapper fileMapper;
    private final MinioClient minioClient;
    private final FileEventProducer fileEventProducer;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.public-endpoint}")
    private String publicEndpoint;
    @Value("${minio.access-key}")
    private String accessKey;
    @Value("${minio.secret-key}")
    private String secretKey;

    /** 专门用来签下载链接的客户端：用对外端点（局域网IP）构建 */
    private MinioClient publicClient;

    /** Bean初始化后执行一次：构建对外客户端 */
    @PostConstruct
    public void init() {
        this.publicClient = MinioClient.builder()
                .endpoint(publicEndpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    /** 上传格式白名单：常见文档/表格/演示/文本/压缩包/图片 */
    private static final Set<String> ALLOWED_EXT = Set.of(
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx",
            "md", "txt", "zip", "rar", "7z",
            "png", "jpg", "jpeg", "gif", "webp");

    @Override
    public FileRecord upload(MultipartFile file, Long kbId, Long docId) {
        AuthUtil.requireWritable();
        String original = file.getOriginalFilename();
        // 格式门卫：前端提示过一遍，后端再兜底一遍，绕不过
        String checkExt = original != null && original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!ALLOWED_EXT.contains(checkExt)) {
            throw new IllegalArgumentException(
                    "不支持的文件格式，目前支持：pdf/word/ppt/excel/md/txt/zip/rar/7z/图片");
        }
        // objectKey=知识库id/UUID.扩展名：原始名留给展示，存储路径防重名
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
        }
        String objectKey = kbId + "/" + UUID.randomUUID() + ext;
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("文件上传MinIO失败：" + e.getMessage());
        }

        FileRecord rec = new FileRecord();
        rec.setKbId(kbId);
        rec.setDocId(docId);
        rec.setOriginalName(original);
        rec.setObjectKey(objectKey);
        rec.setSize(file.getSize());
        rec.setContentType(file.getContentType());
        rec.setUploaderId(UserContext.get().getId());
        fileMapper.insert(rec);

        // 异步事件：主流程到此结束，后续处理消费者慢慢做
        fileEventProducer.sendFileUploaded(rec.getId(), original);
        return rec;
    }

    @Override
    public List<FileRecord> list(Long kbId, Long docId) {
        QueryWrapper<FileRecord> qw = new QueryWrapper<FileRecord>().orderByDesc("create_time");
        if (docId != null) {
            qw.eq("doc_id", docId);
        } else {
            qw.eq("kb_id", kbId);
        }
        return fileMapper.selectList(qw);
    }

    @Override
    public String presignedUrl(Long id) {
        FileRecord rec = mustGet(id);
        try {
            // 签1小时有效的直连URL：用publicClient签，host就是局域网IP；
            // 浏览器直接找MinIO要文件，后端不搬流量
            return publicClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucket)
                    .object(rec.getObjectKey())
                    .method(Method.GET)
                    .expiry(1, TimeUnit.HOURS)
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("生成下载链接失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // 删除权收归管理员
        AuthUtil.requireAdmin();
        FileRecord rec = mustGet(id);
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket).object(rec.getObjectKey()).build());
        } catch (Exception e) {
            throw new IllegalStateException("MinIO删除失败：" + e.getMessage());
        }
        fileMapper.deleteById(id);
    }

    private FileRecord mustGet(Long id) {
        FileRecord rec = fileMapper.selectById(id);
        if (rec == null) {
            throw new IllegalArgumentException("文件不存在");
        }
        return rec;
    }
}