package com.etimesheet.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
public class UserConfig {

    private Long id;

    /** 用户名 */
    private String username;

    /** 姓名 */
    private String name;

    /** 密码 */
    private String password;

    /** 头像（base64） */
    private String avatar;

    /** 公司名称 */
    private String company;

    /** 部门 */
    private String department;

    /** 岗位 */
    private String position;

    /** 常用项目 */
    private String project;

    /** 常用工作内容 */
    private String content;

    /** 薪资模式：HOURLY-按小时计费，FIXED-固定月薪 */
    private String salaryMode;

    /** 小时单价 */
    private BigDecimal hourlyRate;

    /** 固定月薪 */
    private BigDecimal fixedMonthlySalary;

    /** 加班小时单价 */
    private BigDecimal overtimeHourlyRate;

    /** 月度津贴 */
    private BigDecimal allowance;

    /** 月度绩效 */
    private BigDecimal performanceBonus;

    /** 月度扣除 */
    private BigDecimal deduction;

    /** 微信开放平台 openid */
    private String wechatOpenid;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
