package com.etimesheet.controller;

import com.etimesheet.entity.MonthlySummary;
import com.etimesheet.service.AsyncTaskService;
import com.etimesheet.service.MonthlySummaryService;
import com.etimesheet.util.ResponseResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 月度总结 REST API 控制器
 * 支持异步生成总结，避免长时间计算阻塞请求
 */
@RestController
@RequestMapping("/api/monthly-summary")
public class MonthlySummaryController {

    private final MonthlySummaryService monthlySummaryService;
    private final AsyncTaskService asyncTaskService;

    public MonthlySummaryController(MonthlySummaryService monthlySummaryService,
                                    AsyncTaskService asyncTaskService) {
        this.monthlySummaryService = monthlySummaryService;
        this.asyncTaskService = asyncTaskService;
    }

    /**
     * 生成指定月份的总结（同步方式）
     *
     * @param yearMonth 年月 YYYY-MM
     */
    @PostMapping("/generate")
    public ResponseResult<MonthlySummary> generate(
            @RequestParam String yearMonth,
            HttpServletRequest request) {
        validateYearMonth(yearMonth);
        Long userId = (Long) request.getAttribute("userId");
        MonthlySummary summary = monthlySummaryService.generateSummary(userId, yearMonth);
        return ResponseResult.success(summary);
    }

    /**
     * 异步生成指定月份的总结
     * 提交后立即返回任务ID，客户端轮询查询生成结果
     */
    @PostMapping("/generate/async")
    public ResponseResult<Map<String, String>> generateAsync(
            @RequestParam String yearMonth,
            HttpServletRequest request) {
        validateYearMonth(yearMonth);
        Long userId = (Long) request.getAttribute("userId");
        // 提交异步任务
        asyncTaskService.generateSummaryAsync(yearMonth, userId);
        return ResponseResult.success(Map.of(
                "message", "月度总结正在后台异步生成..."
        ));
    }

    /**
     * 获取异步任务状态
     *
     * @param taskId 任务ID
     */
    @GetMapping("/task/{taskId}")
    public ResponseResult<AsyncTaskService.TaskInfo> getTaskStatus(
            @PathVariable String taskId) {
        AsyncTaskService.TaskInfo taskInfo = asyncTaskService.getTask(taskId);
        if (taskInfo == null) {
            return ResponseResult.error("任务不存在");
        }
        return ResponseResult.success(taskInfo);
    }

    /**
     * 尝试自动生成上个月的总结（每月1日自动调用）
     */
    @PostMapping("/try-generate-last-month")
    public ResponseResult<MonthlySummary> tryGenerateLastMonth(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        MonthlySummary summary = monthlySummaryService.tryGenerateLastMonth(userId);
        if (summary == null) {
            return ResponseResult.error(400, "非每月1号无需生成上月总结");
        }
        return ResponseResult.success(summary);
    }

    /**
     * 获取某年的所有月度总结
     */
    @GetMapping("/year/{year}")
    public ResponseResult<List<MonthlySummary>> getByYear(
            @PathVariable Integer year,
            HttpServletRequest request) {
        validateYear(year);
        Long userId = (Long) request.getAttribute("userId");
        List<MonthlySummary> list = monthlySummaryService.getSummariesByYear(userId, year);
        return ResponseResult.success(list);
    }

    /**
     * 获取用户所有有数据的年份
     */
    @GetMapping("/years")
    public ResponseResult<List<Integer>> getYears(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Integer> years = monthlySummaryService.getYears(userId);
        return ResponseResult.success(years);
    }

    /**
     * 导出某年月度总结为Excel
     */
    @GetMapping("/export/{year}")
    public void exportYear(
            @PathVariable Integer year,
            HttpServletRequest request,
            HttpServletResponse response) {
        validateYear(year);
        Long userId = (Long) request.getAttribute("userId");
        monthlySummaryService.exportYearSummary(userId, year, response);
    }

    /**
     * 校验 yearMonth 参数格式（yyyy-MM）
     */
    private void validateYearMonth(String yearMonth) {
        if (yearMonth == null || !yearMonth.matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
            throw new IllegalArgumentException("年月格式错误，应为 yyyy-MM（如 2026-01）");
        }
    }

    /**
     * 校验 year 参数范围
     */
    private void validateYear(Integer year) {
        int currentYear = java.time.LocalDate.now().getYear();
        if (year == null || year < 2000 || year > currentYear + 1) {
            throw new IllegalArgumentException("年份无效，有效范围：2000 ~ " + (currentYear + 1));
        }
    }
}
