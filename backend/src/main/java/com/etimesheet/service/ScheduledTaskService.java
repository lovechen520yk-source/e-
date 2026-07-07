package com.etimesheet.service;

import com.etimesheet.entity.MonthlySummary;
import com.etimesheet.repository.UserConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 定时任务服务
 * 每月1日凌晨2点自动为所有用户生成上月月度总结
 */
@Service
public class ScheduledTaskService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskService.class);

    private final MonthlySummaryService monthlySummaryService;
    private final UserConfigMapper userConfigMapper;

    public ScheduledTaskService(MonthlySummaryService monthlySummaryService,
                                UserConfigMapper userConfigMapper) {
        this.monthlySummaryService = monthlySummaryService;
        this.userConfigMapper = userConfigMapper;
    }

    /**
     * 每月1日凌晨2:00自动为所有用户生成上个月的月度总结
     * cron: 秒 分 时 日 月 星期
     * "0 0 2 1 * ?" 表示每月1日 02:00:00 执行
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void autoGenerateMonthlySummaries() {
        log.info("===== 开始自动生成上月月度总结 =====");

        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        String yearMonth = lastMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        List<Long> userIds = userConfigMapper.findAllUserIds();
        log.info("共找到 {} 个用户，准备生成 {} 的月度总结", userIds.size(), yearMonth);

        int success = 0;
        int failed = 0;

        for (Long userId : userIds) {
            try {
                MonthlySummary summary = monthlySummaryService.generateSummary(userId, yearMonth);
                if (summary != null) {
                    success++;
                    log.info("用户 {} 的 {} 月度总结生成成功，总工时: {}h，总薪资: ¥{}",
                            userId, yearMonth, summary.getTotalHours(), summary.getTotalSalary());
                }
            } catch (Exception e) {
                failed++;
                log.error("用户 {} 的 {} 月度总结生成失败: {}", userId, yearMonth, e.getMessage());
            }
        }

        log.info("===== 月度总结自动生成完成：成功 {} 个，失败 {} 个 =====", success, failed);
    }
}
