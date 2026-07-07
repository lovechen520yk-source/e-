package com.etimesheet.repository;

import com.etimesheet.entity.MonthlySummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 月度总结 MyBatis Mapper
 */
@Mapper
public interface MonthlySummaryMapper {

    /** 插入 */
    int insert(MonthlySummary summary);

    /** 根据用户和年月查询 */
    MonthlySummary findByUserAndMonth(@Param("userId") Long userId, @Param("yearMonth") String yearMonth);

    /** 查询某年的所有月度总结 */
    List<MonthlySummary> findByUserAndYear(@Param("userId") Long userId, @Param("year") Integer year);

    /** 查询用户所有年份（用于Tab） */
    List<Integer> findYearsByUser(@Param("userId") Long userId);

    /** 判断是否存在 */
    boolean existsByUserAndMonth(@Param("userId") Long userId, @Param("yearMonth") String yearMonth);
}
