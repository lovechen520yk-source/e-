package com.etimesheet.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工时记录实体类
 */
@Data
public class WorkRecord {

    private Long id;

    /** 工作日期 */
    private LocalDate workDate;

    /** 公司名称 */
    private String company;

    /** 部门 */
    private String department;

    /** 岗位 */
    private String position;

    /** 工作内容 */
    private String content;

    /** 工时（小时） */
    private BigDecimal hours;

    /** 所属项目 */
    private String project;

    /** 备注 */
    private String remark;

    /** 是否休息 */
    private Boolean isRest;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
