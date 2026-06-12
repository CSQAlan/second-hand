package com.test.secondhand.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.test.secondhand.entity.UploadFile;
import com.test.secondhand.mapper.UploadFileMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class FileCleanScheduler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FileCleanScheduler.class);

    @Autowired
    private UploadFileMapper uploadFileMapper;

    /**
     * 每小时的 0 分执行一次图片垃圾清理任务 (cron = "0 0 * * * ?")
     * 为了方便作业演示，我们把执行周期设定为每 5 分钟执行一次 (cron = "0 0/5 * * * ?")
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    public void cleanUnusedFiles() {
        log.info("[定时任务] 开始执行未关联图片垃圾清理逻辑...");
        
        // 查找 1 小时以前上传但仍未标记为使用的图片（is_used = 0）
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);
        
        List<UploadFile> unusedFiles = uploadFileMapper.selectList(
                new LambdaQueryWrapper<UploadFile>()
                        .eq(UploadFile::getIsUsed, 0)
                        .le(UploadFile::getCreateTime, threshold)
        );

        if (unusedFiles.isEmpty()) {
            log.info("[定时任务] 未发现过期的未使用垃圾文件，本轮清理结束。");
            return;
        }

        log.info("[定时任务] 检索到 {} 个未引用的文件记录，准备彻底清理...", unusedFiles.size());

        int successCount = 0;
        for (UploadFile uf : unusedFiles) {
            try {
                // 1. 物理删除磁盘上的文件
                File file = new File(uf.getFilePath());
                boolean deleted = false;
                if (file.exists()) {
                    deleted = file.delete();
                } else {
                    deleted = true; // 文件在磁盘已不存在，视为删除成功
                }

                if (deleted) {
                    // 2. 从数据库删除记录
                    uploadFileMapper.deleteById(uf.getId());
                    successCount++;
                    log.info("[定时任务] 成功物理清理垃圾图片文件: {}, 原始名: {}", uf.getFilePath(), uf.getFilename());
                } else {
                    log.warn("[定时任务] 磁盘删除物理文件失败: {}", uf.getFilePath());
                }
            } catch (Exception e) {
                log.error("[定时任务] 清理垃圾图片发生异常，文件ID: {}", uf.getId(), e);
            }
        }

        log.info("[定时任务] 垃圾清理执行完毕。应清理 {} 个，成功清理 {} 个。", unusedFiles.size(), successCount);
    }
}
