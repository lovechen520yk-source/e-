package com.etimesheet.repository;

import java.math.BigDecimal;

/**
 * 分组统计结果（对应XML中 SELECT ... AS name, SUM(hours) AS totalHours 的查询结果）
 */
public class GroupStats {

    private String name;
    private BigDecimal totalHours;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(BigDecimal totalHours) {
        this.totalHours = totalHours;
    }
}
