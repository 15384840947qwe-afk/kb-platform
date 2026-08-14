package com.lyq.kb.service;

import com.lyq.kb.entity.FileRecord;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {

    FileRecord upload(MultipartFile file, Long kbId, Long docId);

    /** kbId/docId二选一：按库查或按文档查附件 */
    List<FileRecord> list(Long kbId, Long docId);

    String presignedUrl(Long id);

    void delete(Long id);
}