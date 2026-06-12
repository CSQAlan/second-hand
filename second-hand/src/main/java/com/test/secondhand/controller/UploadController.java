package com.test.secondhand.controller;

import com.test.secondhand.annotation.FastAuthorize;
import com.test.secondhand.common.Result;
import com.test.secondhand.entity.UploadFile;
import com.test.secondhand.mapper.UploadFileMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class UploadController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UploadController.class);

    @Autowired
    private UploadFileMapper uploadFileMapper;

    private static final String UPLOAD_DIR_NAME = "uploads";

    /**
     * 上传图片文件并记录至数据库用于追踪
     */
    @PostMapping("/upload")
    @FastAuthorize(required = true) // 仅登录用户可以上传图片
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("上传文件为空");
        }

        try {
            // 1. 创建本地物理存储目录
            String userDir = System.getProperty("user.dir");
            File uploadDir = new File(userDir, UPLOAD_DIR_NAME);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // 2. 生成随机文件名防止重名冲突
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;

            // 3. 存储文件至物理磁盘
            File destFile = new File(uploadDir, newFilename);
            file.transferTo(destFile);

            // 4. 将文件记录存入数据库追踪，标记为未使用（is_used = 0）
            UploadFile uploadFile = new UploadFile();
            uploadFile.setFilename(originalFilename);
            uploadFile.setFilePath(destFile.getCanonicalPath()); // 存入绝对物理路径，方便清理
            uploadFile.setIsUsed(0); // 默认未使用
            uploadFile.setCreateTime(LocalDateTime.now());
            uploadFileMapper.insert(uploadFile);

            // 5. 返回静态文件访问 URL
            String fileUrl = "http://localhost:8080/" + UPLOAD_DIR_NAME + "/" + newFilename;
            log.info("[文件上传成功] 原始名: {}, 存储路径: {}, 访问URL: {}", originalFilename, destFile.getCanonicalPath(), fileUrl);

            return Result.success(fileUrl);

        } catch (Exception e) {
            log.error("[文件上传失败]", e);
            return Result.error("文件上传失败，请重试");
        }
    }
}
