package com.etimesheet.controller;

import com.etimesheet.service.WorkRecordService;
import com.etimesheet.util.ResponseResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 统计 REST API 控制器
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final WorkRecordService workRecordService;

    public StatsController(WorkRecordService workRecordService) {
        this.workRecordService = workRecordService;
    }

    /**
     * 分组统计工时
     *
     * @param groupBy   分组方式：project/company/department/position
     * @param yearMonth 年月，格式：yyyy-MM
     */
    @GetMapping("/group")
    public ResponseResult<List<Map<String, Object>>> groupStats(
            @RequestParam String groupBy,
            @RequestParam String yearMonth) {
        validateYearMonth(yearMonth);
        List<Map<String, Object>> stats = workRecordService.getGroupStats(groupBy, yearMonth);
        return ResponseResult.success(stats);
    }

    /**
     * 薪资计算
     *
     * @param yearMonth 年月，格式：yyyy-MM
     */
    @GetMapping("/salary")
    public ResponseResult<Map<String, Object>> calculateSalary(
            @RequestParam String yearMonth,
            HttpServletRequest request) {
        validateYearMonth(yearMonth);
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> salaryInfo = workRecordService.calculateSalary(yearMonth, userId);
        return ResponseResult.success(salaryInfo);
    }

    /**
     * 校验 yearMonth 参数格式（yyyy-MM）
     */
    private void validateYearMonth(String yearMonth) {
        if (yearMonth == null || !yearMonth.matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
            throw new IllegalArgumentException("年月格式错误，应为 yyyy-MM（如 2026-01）");
        }
    }
}
