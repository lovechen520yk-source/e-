package com.etimesheet.util;

import com.etimesheet.entity.WorkRecord;
import com.etimesheet.service.WorkRecordService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * JSON格式的备份和恢复工具
 */
@Component
public class BackupUtil {

    private final WorkRecordService workRecordService;
    private final ObjectMapper objectMapper;

    public BackupUtil(WorkRecordService workRecordService) {
        this.workRecordService = workRecordService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 导出全部数据为JSON备份文件
     */
    public void exportBackup(List<WorkRecord> allRecords, HttpServletResponse response) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = URLEncoder.encode("工时备份_" + timestamp, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".json");

            // 构建备份数据结构
            Map<String, Object> backupData = new LinkedHashMap<>();
            backupData.put("exportTime", LocalDateTime.now().toString());
            backupData.put("recordCount", allRecords.size());
            backupData.put("records", allRecords);

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(response.getOutputStream(), backupData);
        } catch (IOException e) {
            throw new RuntimeException("导出备份失败", e);
        }
    }

    /**
     * 解析备份文件（仅解析，不保存）
     * 用于异步导入场景：同步解析文件内容，异步执行数据库写入
     *
     * @param file 上传的JSON文件
     * @return 包含 records 和 total 的 Map
     */
    public Map<String, Object> parseBackupFile(MultipartFile file) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<WorkRecord> successList = new ArrayList<>();
        int failCount = 0;

        try (InputStream inputStream = file.getInputStream()) {
            // 读取备份JSON
            Map<String, Object> backupData = objectMapper.readValue(inputStream,
                    new TypeReference<Map<String, Object>>() {});

            // 提取records列表
            Object recordsObj = backupData.get("records");
            if (recordsObj instanceof List<?>) {
                List<?> rawList = (List<?>) recordsObj;
                for (Object item : rawList) {
                    try {
                        WorkRecord record = objectMapper.convertValue(item, WorkRecord.class);
                        // 重置ID和创建/更新时间，让数据库自动生成
                        record.setId(null);
                        record.setCreatedAt(null);
                        record.setUpdatedAt(null);
                        successList.add(record);
                    } catch (Exception e) {
                        failCount++;
                    }
                }
            }

            result.put("records", successList);
            result.put("total", successList.size() + failCount);
        } catch (IOException e) {
            throw new RuntimeException("解析备份文件失败：" + e.getMessage(), e);
        }

        return result;
    }

    /**
     * 导入JSON备份文件（同步方式）
     *
     * @param file 上传的JSON文件
     * @return 导入结果（总记录数、成功数、失败数）
     */
    public Map<String, Object> importBackup(MultipartFile file) {
        Map<String, Object> parseResult = parseBackupFile(file);
        @SuppressWarnings("unchecked")
        List<WorkRecord> records = (List<WorkRecord>) parseResult.get("records");
        int total = (int) parseResult.get("total");

        // 批量保存
        List<WorkRecord> saved = workRecordService.batchImport(records);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("success", saved.size());
        result.put("fail", total - saved.size());
        result.put("message", "导入完成");
        return result;
    }
}
