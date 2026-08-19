package com.lyq.kb.controller;

import com.lyq.kb.common.Result;
import com.lyq.kb.entity.FileRecord;
import com.lyq.kb.service.FileService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    /** multipart上传：form字段名file，kbId必填，docId选填 */
    @PostMapping("/upload")
    public Result<FileRecord> upload(@RequestParam("file") MultipartFile file,
                                     @RequestParam Long kbId,
                                     @RequestParam(required = false) Long docId) {
        return Result.ok(fileService.upload(file, kbId, docId));
    }

    @GetMapping("/list")
    public Result<List<FileRecord>> list(@RequestParam(required = false) Long kbId,
                                         @RequestParam(required = false) Long docId) {
        return Result.ok(fileService.list(kbId, docId));
    }

    /** 返回1小时有效的MinIO直连下载URL */
    @GetMapping("/{id}/url")
    public Result<String> url(@PathVariable Long id) {
        return Result.ok(fileService.presignedUrl(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        fileService.delete(id);
        return Result.ok();
    }

    /**
     * 图片代理：<img>标签带不了token，此路径已加进拦截器白名单。
     * 存进文档的图片URL是/kb/file/proxy/images/xxx这种相对路径，
     * 任何设备任何网络下都能解析到自己服务器，不绑死IP；
     * Cache-Control 7天，第二次看直接走浏览器缓存
     */
    @GetMapping("/proxy/**")
    public void proxy(HttpServletRequest request, HttpServletResponse response) {
        String uri = request.getRequestURI();
        String marker = "/file/proxy/";
        int idx = uri.indexOf(marker);
        if (idx < 0) {
            response.setStatus(404);
            return;
        }
        String key = URLDecoder.decode(uri.substring(idx + marker.length()), StandardCharsets.UTF_8);
        // 只允许images/前缀，防路径穿越
        if (!key.startsWith("images/")) {
            response.setStatus(404);
            return;
        }
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder().bucket(bucket).object(key).build());
            response.setContentType(stat.contentType());
            response.setHeader("Cache-Control", "public, max-age=604800");
            response.setContentLengthLong(stat.size());
            try (InputStream in = minioClient.getObject(
                    GetObjectArgs.builder().bucket(bucket).object(key).build())) {
                in.transferTo(response.getOutputStream());
            }
        } catch (Exception e) {
            response.setStatus(404);
        }
    }
}