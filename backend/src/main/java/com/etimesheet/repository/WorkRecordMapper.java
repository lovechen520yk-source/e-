package com.etimesheet.repository;

import com.etimesheet.entity.WorkRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 工时记录 MyBatis Mapper
 */
@Mapper
public interface WorkRecordMapper {

    /** 插入记录 */
    int insert(WorkRecord record);

    /** 批量插入记录 */
    int batchInsert(List<WorkRecord> records);

    /** 根据ID更新 */
    int updateById(WorkRecord record);

    /** 根据ID删除 */
    int deleteById(Long id);

    /** 批量删除 */
    int batchDeleteByIds(List<Long> ids);

    /** 根据ID查询 */
    WorkRecord findById(Long id);

    /** 查询所有 */
    List<WorkRecord> findAll();

    /** 按年月查询 */
    List<WorkRecord> findByYearMonth(@Param("yearMonth") String yearMonth);

    /** 按日期范围查询 */
    List<WorkRecord> findByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 检查是否存在 */
    boolean existsById(Long id);

    /** 按项目分组统计 */
    List<GroupStats> sumHoursGroupByProject(@Param("yearMonth") String yearMonth);

    /** 按公司分组统计 */
    List<GroupStats> sumHoursGroupByCompany(@Param("yearMonth") String yearMonth);

    /** 按部门分组统计 */
    List<GroupStats> sumHoursGroupByDepartment(@Param("yearMonth") String yearMonth);

    /** 按岗位分组统计 */
    List<GroupStats> sumHoursGroupByPosition(@Param("yearMonth") String yearMonth);

    /** 某月份总工时 */
    BigDecimal totalHoursByYearMonth(@Param("yearMonth") String yearMonth);

    /** 某月份记录数 */
    long countByYearMonth(@Param("yearMonth") String yearMonth);

    /** 去重项目列表 */
    List<String> findDistinctProjects();

    /** 去重公司列表 */
    List<String> findDistinctCompanies();

    /** 去重部门列表 */
    List<String> findDistinctDepartments();

    /** 去重岗位列表 */
    List<String> findDistinctPositions();
}
