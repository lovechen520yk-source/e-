package com.etimesheet.controller;

import com.etimesheet.entity.WorkRecord;
import com.etimesheet.service.AsyncTaskService;
import com.etimesheet.service.WorkRecordService;
import com.etimesheet.util.BackupUtil;
import com.etimesheet.util.ExcelUtil;
import com.etimesheet.util.ResponseResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 导出和备份 REST API 控制器
 * 支持异步任务处理，避免长时间操作阻塞请求线程
 */
@RestController
@RequestMapping("/api")
public class ExportController {

    private final WorkRecordService workRecordService;
    private final ExcelUtil excelUtil;
    private final BackupUtil backupUtil;
    private final AsyncTaskService asyncTaskService;

    public ExportController(WorkRecordService workRecordService,
                            ExcelUtil excelUtil,
                            BackupUtil backupUtil,
                            AsyncTaskService asyncTaskService) {
        this.workRecordService = workRecordService;
        this.excelUtil = excelUtil;
        this.backupUtil = backupUtil;
        this.asyncTaskService = asyncTaskService;
    }

    /**
     * 导出Excel文件（同步方式，数据量小时使用）
     *
     * @param yearMonth 年月，格式：yyyy-MM
     */
    @GetMapping("/export/excel")
    public void exportExcel(@RequestParam String yearMonth, HttpServletResponse response) {
        List<WorkRecord> records = workRecordService.getRecordsByYearMonth(yearMonth);
        excelUtil.exportWorkRecords(records, yearMonth, response);
    }

    /**
     * 下载JSON备份（同步方式）
     */
    @GetMapping("/backup/download")
    public void downloadBackup(HttpServletResponse response) {
        List<WorkRecord> allRecords = workRecordService.getAllRecords();
        backupUtil.exportBackup(allRecords, response);
    }

    /**
     * 异步导入JSON备份
     * 提交后立即返回任务ID，客户端轮询查询导入进度
     */
    @PostMapping("/backup/import")
    public ResponseResult<Map<String, Object>> importBackup(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        // 同步解析文件，异步执行导入
        Map<String, Object> parseResult = backupUtil.parseBackupFile(file);
        @SuppressWarnings("unchecked")
        List<WorkRecord> records = (List<WorkRecord>) parseResult.get("records");
        Long userId = (Long) request.getAttribute("userId");

        // 提交异步导入任务
        asyncTaskService.importBackupAsync(records, userId);

        Map<String, Object> result = Map.of(
                "total", parseResult.get("total"),
                "message", "导入任务已提交，正在后台异步执行..."
        );
        return ResponseResult.success(result);
    }
}
