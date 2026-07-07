package com.etimesheet.controller;

import com.etimesheet.dto.AddWorkRecordDTO;
import com.etimesheet.entity.WorkRecord;
import com.etimesheet.service.WorkRecordService;
import com.etimesheet.util.ResponseResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 工时记录 REST API 控制器
 */
@RestController
@RequestMapping("/api/records")
public class WorkRecordController {

    private final WorkRecordService workRecordService;

    public WorkRecordController(WorkRecordService workRecordService) {
        this.workRecordService = workRecordService;
    }

    /**
     * 添加记录
     */
    @PostMapping
    public ResponseResult<WorkRecord> addRecord(@Valid @RequestBody AddWorkRecordDTO dto) {
        WorkRecord record = workRecordService.addRecord(dto);
        return ResponseResult.success(record);
    }

    /**
     * 查询记录列表，支持yearMonth参数
     */
    @GetMapping
    public ResponseResult<List<WorkRecord>> getRecords(
            @RequestParam(required = false) String yearMonth) {
        List<WorkRecord> records;
        if (yearMonth != null && !yearMonth.isEmpty()) {
            records = workRecordService.getRecordsByYearMonth(yearMonth);
        } else {
            records = workRecordService.getAllRecords();
        }
        return ResponseResult.success(records);
    }

    /**
     * 获取单条记录
     */
    @GetMapping("/{id}")
    public ResponseResult<WorkRecord> getRecord(@PathVariable Long id) {
        WorkRecord record = workRecordService.getRecord(id);
        return ResponseResult.success(record);
    }

    /**
     * 更新记录
     */
    @PutMapping("/{id}")
    public ResponseResult<WorkRecord> updateRecord(
            @PathVariable Long id, @Valid @RequestBody AddWorkRecordDTO dto) {
        WorkRecord record = workRecordService.updateRecord(id, dto);
        return ResponseResult.success(record);
    }

    /**
     * 删除单条记录
     */
    @DeleteMapping("/{id}")
    public ResponseResult<Void> deleteRecord(@PathVariable Long id) {
        workRecordService.deleteRecord(id);
        return ResponseResult.success(null);
    }

    /**
     * 批量删除记录（接收id列表）
     */
    @DeleteMapping
    public ResponseResult<Void> batchDelete(@RequestBody Map<String, List<Long>> request) {
        List<Long> ids = request.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ResponseResult.error("ids不能为空");
        }
        workRecordService.batchDelete(ids);
        return ResponseResult.success(null);
    }

    /**
     * 批量删除记录（POST方式，与前端batchDeleteRecords接口匹配）
     */
    @PostMapping("/batch-delete")
    public ResponseResult<Void> batchDeletePost(@RequestBody Map<String, List<Long>> request) {
        List<Long> ids = request.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ResponseResult.error("ids不能为空");
        }
        workRecordService.batchDelete(ids);
        return ResponseResult.success(null);
    }
}
