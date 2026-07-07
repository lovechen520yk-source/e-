package com.etimesheet.repository;

import com.etimesheet.entity.UserConfig;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 用户配置 MyBatis Mapper
 */
@Mapper
public interface UserConfigMapper {

    /** 根据ID查询 */
    UserConfig findById(Long id);

    /** 根据用户名查询 */
    UserConfig findByUsername(String username);

    /** 根据微信 openid 查询 */
    UserConfig findByWechatOpenid(String wechatOpenid);

    /** 插入配置 */
    int insert(UserConfig config);

    /** 更新配置 */
    int updateById(UserConfig config);

    /** 查询所有用户ID（用于定时任务） */
    List<Long> findAllUserIds();
}
