package com.etimesheet.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.etimesheet.entity.MonthlySummary;
import com.etimesheet.entity.UserConfig;
import com.etimesheet.entity.WorkRecord;
import com.etimesheet.repository.MonthlySummaryMapper;
import com.etimesheet.repository.WorkRecordMapper;
import com.etimesheet.util.BusinessException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 月度总结业务逻辑层
 */
@Service
public class MonthlySummaryService {

    private final MonthlySummaryMapper monthlySummaryMapper;
    private final WorkRecordMapper workRecordMapper;
    private final UserConfigService userConfigService;

    public MonthlySummaryService(MonthlySummaryMapper monthlySummaryMapper,
                                 WorkRecordMapper workRecordMapper,
                                 UserConfigService userConfigService) {
        this.monthlySummaryMapper = monthlySummaryMapper;
        this.workRecordMapper = workRecordMapper;
        this.userConfigService = userConfigService;
    }

    /**
     * 为指定用户生成指定月份的总结
     *
     * @param userId    用户ID
     * @param yearMonth 年月 YYYY-MM
     * @return 生成的总结，若已存在则直接返回
     */
    @Transactional
    @CacheEvict(value = "monthlySummary", allEntries = true)
    public MonthlySummary generateSummary(Long userId, String yearMonth) {
        // 已存在则直接返回
        if (monthlySummaryMapper.existsByUserAndMonth(userId, yearMonth)) {
            return monthlySummaryMapper.findByUserAndMonth(userId, yearMonth);
        }

        // 查询该月所有记录
        List<WorkRecord> records = workRecordMapper.findByYearMonth(yearMonth);
        BigDecimal totalHours = records.stream()
                .map(WorkRecord::getHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算该月薪资
        UserConfig config = userConfigService.getConfig(userId);
        BigDecimal salary = calculateSalary(records, totalHours, config, yearMonth);

        // 记录条数
        long count = workRecordMapper.countByYearMonth(yearMonth);

        // 生成年份
        int year = Integer.parseInt(yearMonth.substring(0, 4));

        MonthlySummary summary = new MonthlySummary();
        summary.setUserId(userId);
        summary.setYearMonth(yearMonth);
        summary.setYear(year);
        summary.setTotalHours(totalHours.setScale(2, RoundingMode.HALF_UP));
        summary.setTotalSalary(salary.setScale(2, RoundingMode.HALF_UP));
        summary.setRecordCount((int) count);

        monthlySummaryMapper.insert(summary);
        return summary;
    }

    /**
     * 获取某年的所有月度总结
     */
    @Cacheable(value = "monthlySummary", key = "#userId + ':' + #year")
    public List<MonthlySummary> getSummariesByYear(Long userId, Integer year) {
        return monthlySummaryMapper.findByUserAndYear(userId, year);
    }

    /**
     * 获取用户的所有年份
     */
    @Cacheable(value = "monthlySummary", key = "'years:' + #userId")
    public List<Integer> getYears(Long userId) {
        List<Integer> years = monthlySummaryMapper.findYearsByUser(userId);
        // 若没有数据，返回当前年份
        if (years.isEmpty()) {
            years.add(LocalDate.now().getYear());
        }
        return years;
    }

    /**
     * 尝试自动生成上个月的总结（每月1日调用）
     */
    public MonthlySummary tryGenerateLastMonth(Long userId) {
        LocalDate today = LocalDate.now();
        int currentDay = today.getDayOfMonth();

        // 只在每月1日生成上个月的总结
        if (currentDay != 1) {
            return null;
        }

        LocalDate lastMonth = today.minusMonths(1);
        String yearMonth = lastMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return generateSummary(userId, yearMonth);
    }

    /**
     * 导出某年月度总结为Excel
     */
    public void exportYearSummary(Long userId, Integer year, HttpServletResponse response) {
        List<MonthlySummary> summaries = monthlySummaryMapper.findByUserAndYear(userId, year);

        if (summaries.isEmpty()) {
            throw new BusinessException(year + "年没有月度总结数据");
        }

        try {
            String fileName = URLEncoder.encode("月度总结_" + year, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            List<SummaryExcelVO> voList = new ArrayList<>();
            for (MonthlySummary s : summaries) {
                SummaryExcelVO vo = new SummaryExcelVO();
                vo.setYearMonth(s.getYearMonth());
                vo.setRecordCount(s.getRecordCount());
                vo.setTotalHours(s.getTotalHours());
                vo.setTotalSalary(s.getTotalSalary());
                voList.add(vo);
            }

            EasyExcel.write(response.getOutputStream(), SummaryExcelVO.class)
                    .sheet(year + "年月度总结")
                    .doWrite(voList);
        } catch (IOException e) {
            throw new RuntimeException("导出Excel失败", e);
        }
    }

    /**
     * 薪资计算逻辑（与 WorkRecordService 保持一致）
     * HOURLY：总工时 × 小时单价
     * FIXED：按天计算，>=8h按日薪+加班，<8h按时薪比例
     */
    private BigDecimal calculateSalary(List<WorkRecord> records, BigDecimal totalHours,
                                        UserConfig config, String yearMonth) {
        if ("HOURLY".equals(config.getSalaryMode())) {
            BigDecimal hourlyRate = config.getHourlyRate() != null ? config.getHourlyRate() : BigDecimal.ZERO;
            return totalHours.multiply(hourlyRate).setScale(2, RoundingMode.HALF_UP);
        } else {
            BigDecimal fixedSalary = config.getFixedMonthlySalary() != null
                    ? config.getFixedMonthlySalary() : BigDecimal.ZERO;
            BigDecimal overtimeRate = config.getOvertimeHourlyRate() != null
                    ? config.getOvertimeHourlyRate() : BigDecimal.ZERO;

            int year = Integer.parseInt(yearMonth.substring(0, 4));
            int month = Integer.parseInt(yearMonth.substring(5, 7));
            int daysInMonth = java.time.YearMonth.of(year, month).lengthOfMonth();

            BigDecimal dailySalary = fixedSalary.divide(
                    BigDecimal.valueOf(daysInMonth), 4, RoundingMode.HALF_UP);
            BigDecimal hourlyRateFromFixed = dailySalary.divide(
                    BigDecimal.valueOf(8), 4, RoundingMode.HALF_UP);

            java.util.Map<String, BigDecimal> dailyHours = new java.util.HashMap<>();
            for (WorkRecord record : records) {
                dailyHours.merge(record.getWorkDate().toString(), record.getHours(), BigDecimal::add);
            }

            BigDecimal totalSalary = BigDecimal.ZERO;
            for (BigDecimal hours : dailyHours.values()) {
                if (hours.compareTo(BigDecimal.valueOf(8)) >= 0) {
                    totalSalary = totalSalary.add(dailySalary)
                            .add(hours.subtract(BigDecimal.valueOf(8)).multiply(overtimeRate));
                } else {
                    totalSalary = totalSalary.add(hourlyRateFromFixed.multiply(hours));
                }
            }
            return totalSalary.setScale(2, RoundingMode.HALF_UP);
        }
    }

    @Data
    static class SummaryExcelVO {
        @ExcelProperty("月份")
        private String yearMonth;

        @ExcelProperty("记录条数")
        private Integer recordCount;

        @ExcelProperty("总工时(h)")
        private BigDecimal totalHours;

        @ExcelProperty("总薪资(¥)")
        private BigDecimal totalSalary;
    }
}
