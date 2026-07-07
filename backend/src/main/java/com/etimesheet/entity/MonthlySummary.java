package com.etimesheet.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 月度薪资总结实体类
 */
@Data
public class MonthlySummary {

    private Long id;

    /** 用户ID */
    private Long userId;

    /** 年月 YYYY-MM */
    private String yearMonth;

    /** 年份，用于按年筛选 */
    private Integer year;

    /** 总工时 */
    private BigDecimal totalHours;

    /** 总薪资 */
    private BigDecimal totalSalary;

    /** 记录条数 */
    private Integer recordCount;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
