package com.etimesheet.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 添加工时记录请求体
 */
@Data
public class AddWorkRecordDTO {

    @NotNull(message = "工作日期不能为空")
    private LocalDate workDate;

    @NotBlank(message = "公司名称不能为空")
    @Size(max = 100)
    private String company;

    @NotBlank(message = "部门不能为空")
    @Size(max = 100)
    private String department;

    @NotBlank(message = "岗位不能为空")
    @Size(max = 100)
    private String position;

    @Size(max = 500)
    private String content;

    @DecimalMax(value = "24.0", message = "工时不能超过24")
    private BigDecimal hours;

    @Size(max = 100)
    private String project;

    @Size(max = 500)
    private String remark;

    /** 是否休息 */
    private Boolean isRest;
}

/**
 * 更新工时记录请求体
 */
@Data
class UpdateWorkRecordDTO {

    @NotNull(message = "记录ID不能为空")
    private Long id;

    @NotNull(message = "工作日期不能为空")
    private LocalDate workDate;

    @NotBlank(message = "公司名称不能为空")
    @Size(max = 100)
    private String company;

    @NotBlank(message = "部门不能为空")
    @Size(max = 100)
    private String department;

    @NotBlank(message = "岗位不能为空")
    @Size(max = 100)
    private String position;

    @NotBlank(message = "工作内容不能为空")
    @Size(max = 500)
    private String content;

    @NotNull(message = "工时不能为空")
    @DecimalMin(value = "0.1", message = "工时不能小于0.1")
    @DecimalMax(value = "24.0", message = "工时不能超过24")
    private BigDecimal hours;

    @NotBlank(message = "项目名称不能为空")
    @Size(max = 100)
    private String project;

    @Size(max = 500)
    private String remark;
}

/**
 * 查询参数
 */
@Data
class QueryParams {
    /** 年月，格式：yyyy-MM */
    private String yearMonth;

    /** 公司 */
    private String company;

    /** 部门 */
    private String department;

    /** 项目 */
    private String project;

    /** 页码（从1开始） */
    private int page = 1;

    /** 每页大小 */
    private int size = 20;
}

/**
 * 统计查询参数
 */
@Data
class StatsQueryDTO {
    /** 分组方式：project/company/department/position */
    @NotBlank
    private String groupBy;

    /** 年月，格式：yyyy-MM */
    @NotBlank
    private String yearMonth;
}

/**
 * 薪资查询参数
 */
@Data
class SalaryQueryDTO {
    /** 年月，格式：yyyy-MM */
    @NotBlank
    private String yearMonth;
}
