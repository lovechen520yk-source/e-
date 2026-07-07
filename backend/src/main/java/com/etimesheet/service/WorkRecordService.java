package com.etimesheet.service;

import com.etimesheet.dto.AddWorkRecordDTO;
import com.etimesheet.entity.UserConfig;
import com.etimesheet.entity.WorkRecord;
import com.etimesheet.repository.WorkRecordMapper;
import com.etimesheet.util.BusinessException;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 工时记录业务逻辑层
 */
@Service
public class WorkRecordService {

    private final WorkRecordMapper workRecordMapper;
    private final UserConfigService userConfigService;

    public WorkRecordService(WorkRecordMapper workRecordMapper,
                             UserConfigService userConfigService) {
        this.workRecordMapper = workRecordMapper;
        this.userConfigService = userConfigService;
    }

    /**
     * 添加记录
     */
    @Transactional
    @CacheEvict(value = {"workRecords", "stats", "metaOptions"}, allEntries = true)
    public WorkRecord addRecord(AddWorkRecordDTO dto) {
        // 非休息记录校验
        if (dto.getIsRest() == null || !dto.getIsRest()) {
            if (dto.getContent() == null || dto.getContent().isBlank()) {
                throw new BusinessException("工作内容不能为空");
            }
            if (dto.getProject() == null || dto.getProject().isBlank()) {
                throw new BusinessException("项目名称不能为空");
            }
            if (dto.getHours() == null || dto.getHours().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("工时不能为空且必须大于0");
            }
        }
        WorkRecord record = new WorkRecord();
        BeanUtils.copyProperties(dto, record);
        workRecordMapper.insert(record);
        return record;
    }

    /**
     * 更新记录
     */
    @Transactional
    @CacheEvict(value = {"workRecords", "stats", "metaOptions"}, allEntries = true)
    public WorkRecord updateRecord(Long id, AddWorkRecordDTO dto) {
        // 非休息记录校验
        if (dto.getIsRest() == null || !dto.getIsRest()) {
            if (dto.getContent() == null || dto.getContent().isBlank()) {
                throw new BusinessException("工作内容不能为空");
            }
            if (dto.getProject() == null || dto.getProject().isBlank()) {
                throw new BusinessException("项目名称不能为空");
            }
            if (dto.getHours() == null || dto.getHours().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("工时不能为空且必须大于0");
            }
        }
        WorkRecord record = workRecordMapper.findById(id);
        if (record == null) {
            throw new BusinessException("记录不存在，ID：" + id);
        }
        BeanUtils.copyProperties(dto, record);
        record.setId(id);
        workRecordMapper.updateById(record);
        return record;
    }

    /**
     * 删除单条记录
     */
    @Transactional
    @CacheEvict(value = {"workRecords", "stats", "metaOptions"}, allEntries = true)
    public void deleteRecord(Long id) {
        if (!workRecordMapper.existsById(id)) {
            throw new BusinessException("记录不存在，ID：" + id);
        }
        workRecordMapper.deleteById(id);
    }

    /**
     * 批量删除记录
     */
    @Transactional
    @CacheEvict(value = {"workRecords", "stats", "metaOptions"}, allEntries = true)
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        workRecordMapper.batchDeleteByIds(ids);
    }

    /**
     * 批量导入记录（用于备份恢复，使用 batchInsert 大幅提升性能）
     */
    @Transactional
    @CacheEvict(value = {"workRecords", "stats", "metaOptions"}, allEntries = true)
    public List<WorkRecord> batchImport(List<WorkRecord> records) {
        if (records.isEmpty()) return records;
        // 使用 MyBatis 批量 INSERT，配合 JDBC rewriteBatchedStatements 参数
        workRecordMapper.batchInsert(records);
        return records;
    }

    /**
     * 根据ID获取记录
     */
    @Cacheable(value = "workRecords", key = "#id")
    public WorkRecord getRecord(Long id) {
        WorkRecord record = workRecordMapper.findById(id);
        if (record == null) {
            throw new BusinessException("记录不存在，ID：" + id);
        }
        return record;
    }

    /**
     * 查询所有记录
     */
    @Cacheable(value = "workRecords", key = "'all'")
    public List<WorkRecord> getAllRecords() {
        return workRecordMapper.findAll();
    }

    /**
     * 按年月查询记录
     */
    @Cacheable(value = "workRecords", key = "'month:' + #yearMonth")
    public List<WorkRecord> getRecordsByYearMonth(String yearMonth) {
        return workRecordMapper.findByYearMonth(yearMonth);
    }

    /**
     * 分组统计工时
     *
     * @param groupBy   分组字段：project/company/department/position
     * @param yearMonth 年月
     * @return 分组统计结果
     */
    @Cacheable(value = "stats", key = "#groupBy + ':' + #yearMonth")
    public List<Map<String, Object>> getGroupStats(String groupBy, String yearMonth) {
        List<com.etimesheet.repository.GroupStats> statsList;
        switch (groupBy) {
            case "project":
                statsList = workRecordMapper.sumHoursGroupByProject(yearMonth);
                break;
            case "company":
                statsList = workRecordMapper.sumHoursGroupByCompany(yearMonth);
                break;
            case "department":
                statsList = workRecordMapper.sumHoursGroupByDepartment(yearMonth);
                break;
            case "position":
                statsList = workRecordMapper.sumHoursGroupByPosition(yearMonth);
                break;
            default:
                throw new BusinessException("不支持的分组方式：" + groupBy);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (com.etimesheet.repository.GroupStats stat : statsList) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", stat.getName());
            item.put("totalHours", stat.getTotalHours());
            result.add(item);
        }
        return result;
    }

    /**
     * 薪资计算
     * <p>
     * HOURLY模式：总工时 × 小时单价
     * FIXED模式：按天计算
     *   - 工时>=8的天：日薪 + (工时-8) × 加班工价
     *   - 工时<8的天：时薪 × 工时（时薪 = 底薪/月天数/8）
     */
    @Cacheable(value = "stats", key = "'salary:' + #yearMonth + ':' + #userId")
    public Map<String, Object> calculateSalary(String yearMonth, Long userId) {
        UserConfig config = userConfigService.getConfig(userId);
        List<WorkRecord> records = workRecordMapper.findByYearMonth(yearMonth);
        BigDecimal totalHours = records.stream()
                .map(WorkRecord::getHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long recordCount = workRecordMapper.countByYearMonth(yearMonth);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("yearMonth", yearMonth);
        result.put("totalHours", totalHours);
        result.put("recordCount", recordCount);
        result.put("salaryMode", config.getSalaryMode());

        BigDecimal salary = BigDecimal.ZERO;
        String description;

        if ("HOURLY".equals(config.getSalaryMode())) {
            BigDecimal hourlyRate = config.getHourlyRate() != null ? config.getHourlyRate() : BigDecimal.ZERO;
            salary = totalHours.multiply(hourlyRate).setScale(2, RoundingMode.HALF_UP);
            description = String.format("按小时计费：%.1f小时 × %.2f元/小时 = %.2f元",
                    totalHours, hourlyRate, salary);
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

            Map<String, BigDecimal> dailyHours = new HashMap<>();
            for (WorkRecord record : records) {
                dailyHours.merge(record.getWorkDate().toString(), record.getHours(), BigDecimal::add);
            }

            BigDecimal normalPay = BigDecimal.ZERO;
            BigDecimal overtimePay = BigDecimal.ZERO;
            for (BigDecimal hours : dailyHours.values()) {
                if (hours.compareTo(BigDecimal.valueOf(8)) >= 0) {
                    normalPay = normalPay.add(dailySalary);
                    overtimePay = overtimePay.add(
                            hours.subtract(BigDecimal.valueOf(8)).multiply(overtimeRate));
                } else {
                    normalPay = normalPay.add(hourlyRateFromFixed.multiply(hours));
                }
            }
            salary = normalPay.add(overtimePay).setScale(2, RoundingMode.HALF_UP);
            description = String.format("固定月薪：日薪%.2f元 + 加班费%.2f元 = %.2f元（月%d天）",
                    normalPay, overtimePay, salary, daysInMonth);
        }

        result.put("salary", salary);
        result.put("description", description);
        return result;
    }

    /**
     * 获取去重项目列表（结果缓存5分钟，数据变更极少）
     */
    @Cacheable(value = "metaOptions", key = "'projects'")
    public List<String> getDistinctProjects() {
        return workRecordMapper.findDistinctProjects();
    }

    /**
     * 获取去重公司列表（结果缓存5分钟）
     */
    @Cacheable(value = "metaOptions", key = "'companies'")
    public List<String> getDistinctCompanies() {
        return workRecordMapper.findDistinctCompanies();
    }

    /**
     * 获取去重部门列表（结果缓存5分钟）
     */
    @Cacheable(value = "metaOptions", key = "'departments'")
    public List<String> getDistinctDepartments() {
        return workRecordMapper.findDistinctDepartments();
    }

    /**
     * 获取去重岗位列表（结果缓存5分钟）
     */
    @Cacheable(value = "metaOptions", key = "'positions'")
    public List<String> getDistinctPositions() {
        return workRecordMapper.findDistinctPositions();
    }

    /**
     * 获取所有选项（结果缓存5分钟）
     */
    @Cacheable(value = "metaOptions", key = "'allOptions'")
    public Map<String, List<String>> getAllOptions() {
        Map<String, List<String>> options = new LinkedHashMap<>();
        options.put("projects", getDistinctProjects());
        options.put("companies", getDistinctCompanies());
        options.put("departments", getDistinctDepartments());
        options.put("positions", getDistinctPositions());
        return options;
    }
}
