package com.etimesheet.service;

import com.etimesheet.entity.MonthlySummary;
import com.etimesheet.entity.WorkRecord;
import com.etimesheet.util.ExcelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 异步任务服务 - 管理长时间运行的后台任务
 * 支持多用户提交异步任务，并跟踪任务执行状态
 */
@Service
public class AsyncTaskService {

    private static final Logger log = LoggerFactory.getLogger(AsyncTaskService.class);

    private final AtomicLong taskIdCounter = new AtomicLong(0);
    private final ConcurrentHashMap<String, TaskInfo> taskMap = new ConcurrentHashMap<>();

    private final WorkRecordService workRecordService;
    private final MonthlySummaryService monthlySummaryService;
    private final ExcelUtil excelUtil;

    public AsyncTaskService(WorkRecordService workRecordService,
                            MonthlySummaryService monthlySummaryService,
                            ExcelUtil excelUtil) {
        this.workRecordService = workRecordService;
        this.monthlySummaryService = monthlySummaryService;
        this.excelUtil = excelUtil;
    }

    // ==================== 任务状态管理 ====================

    /**
     * 获取任务信息
     */
    public TaskInfo getTask(String taskId) {
        return taskMap.get(taskId);
    }

    /**
     * 生成唯一任务ID
     */
    private String generateTaskId() {
        return "TASK-" + System.currentTimeMillis() + "-" + taskIdCounter.incrementAndGet();
    }

    /**
     * 创建并注册任务
     */
    private TaskInfo registerTask(TaskType type, Long userId) {
        String taskId = generateTaskId();
        TaskInfo info = new TaskInfo(taskId, type, userId);
        taskMap.put(taskId, info);
        return info;
    }

    /**
     * 更新任务进度
     */
    private void updateProgress(String taskId, int progress, String message) {
        TaskInfo info = taskMap.get(taskId);
        if (info != null) {
            info.setProgress(progress);
            info.setMessage(message);
        }
    }

    // ==================== 异步操作方法（非阻塞返回任务ID） ====================

    /**
     * 异步导入备份（返回任务ID）
     */
    @Async("asyncExecutor")
    public CompletableFuture<String> importBackupAsync(List<WorkRecord> records, Long userId) {
        TaskInfo task = registerTask(TaskType.IMPORT_BACKUP, userId);
        String taskId = task.getTaskId();
        task.setStatus(TaskStatus.RUNNING);
        updateProgress(taskId, 0, "开始导入...");

        try {
            int total = records.size();
            // 分批处理，每批100条，更新进度
            int batchSize = 100;
            for (int i = 0; i < total; i += batchSize) {
                int end = Math.min(i + batchSize, total);
                List<WorkRecord> batch = records.subList(i, end);
                workRecordService.batchImport(batch);
                int progress = (int) ((double) end / total * 100);
                updateProgress(taskId, progress, "已导入 " + end + "/" + total + " 条");
            }

            task.setStatus(TaskStatus.COMPLETED);
            updateProgress(taskId, 100, "导入完成，共 " + total + " 条");
            log.info("异步导入备份完成，taskId={}, userId={}, total={}", taskId, userId, total);
        } catch (Exception e) {
            task.setStatus(TaskStatus.FAILED);
            task.setMessage("导入失败: " + e.getMessage());
            log.error("异步导入备份失败，taskId={}, userId={}", taskId, userId, e);
        }

        return CompletableFuture.completedFuture(taskId);
    }

    /**
     * 异步生成月度总结
     */
    @Async("asyncExecutor")
    public CompletableFuture<String> generateSummaryAsync(String yearMonth, Long userId) {
        TaskInfo task = registerTask(TaskType.GENERATE_SUMMARY, userId);
        String taskId = task.getTaskId();
        task.setStatus(TaskStatus.RUNNING);
        updateProgress(taskId, 0, "开始生成 " + yearMonth + " 月度总结...");

        try {
            MonthlySummary summary = monthlySummaryService.generateSummary(userId, yearMonth);
            task.setStatus(TaskStatus.COMPLETED);
            task.setResult(summary);
            updateProgress(taskId, 100, "月度总结生成完成");
            log.info("异步生成月度总结完成，taskId={}, userId={}, yearMonth={}", taskId, userId, yearMonth);
        } catch (Exception e) {
            task.setStatus(TaskStatus.FAILED);
            task.setMessage("生成失败: " + e.getMessage());
            log.error("异步生成月度总结失败，taskId={}, userId={}, yearMonth={}", taskId, userId, yearMonth, e);
        }

        return CompletableFuture.completedFuture(taskId);
    }

    // ==================== 内部数据类型 ====================

    public enum TaskType {
        EXPORT_EXCEL,
        IMPORT_BACKUP,
        GENERATE_SUMMARY,
        EXPORT_SUMMARY
    }

    public enum TaskStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED
    }

    /**
     * 任务信息
     */
    public static class TaskInfo {
        private final String taskId;
        private final TaskType type;
        private final Long userId;
        private final LocalDateTime createdAt;
        private TaskStatus status;
        private int progress;
        private String message;
        private Object result;

        public TaskInfo(String taskId, TaskType type, Long userId) {
            this.taskId = taskId;
            this.type = type;
            this.userId = userId;
            this.createdAt = LocalDateTime.now();
            this.status = TaskStatus.PENDING;
            this.progress = 0;
            this.message = "任务已创建";
        }

        public String getTaskId() { return taskId; }
        public TaskType getType() { return type; }
        public Long getUserId() { return userId; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public TaskStatus getStatus() { return status; }
        public void setStatus(TaskStatus status) { this.status = status; }
        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Object getResult() { return result; }
        public void setResult(Object result) { this.result = result; }
    }
}
