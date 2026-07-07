package com.etimesheet.service;

import com.etimesheet.entity.UserConfig;
import com.etimesheet.repository.UserConfigMapper;
import com.etimesheet.util.BusinessException;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户配置业务逻辑层
 */
@Service
public class UserConfigService {

    private final UserConfigMapper userConfigMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserConfigService(UserConfigMapper userConfigMapper) {
        this.userConfigMapper = userConfigMapper;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * 根据用户ID获取用户配置
     */
    @Cacheable(value = "userConfig", key = "#userId")
    public UserConfig getConfig(Long userId) {
        UserConfig config = userConfigMapper.findById(userId);
        if (config == null) {
            config = new UserConfig();
            config.setId(userId);
            config.setCompany("");
            config.setDepartment("");
            config.setPosition("");
            config.setProject("");
            config.setContent("");
            config.setSalaryMode("FIXED");
            userConfigMapper.insert(config);
        }
        return config;
    }

    /**
     * 保存/更新用户配置
     */
    @Transactional
    @CacheEvict(value = "userConfig", key = "#newConfig.id")
    public UserConfig saveConfig(UserConfig newConfig) {
        UserConfig config = userConfigMapper.findById(newConfig.getId());
        if (config != null) {
            // 保留密码不被覆盖
            String password = config.getPassword();
            BeanUtils.copyProperties(newConfig, config, "id", "password", "updatedAt");
            config.setPassword(password);
            userConfigMapper.updateById(config);
        } else {
            config = newConfig;
            if (config.getSalaryMode() == null) {
                config.setSalaryMode("FIXED");
            }
            userConfigMapper.insert(config);
        }
        return config;
    }

    /**
     * 修改密码
     *
     * @param userId           用户ID
     * @param originalPassword 原密码
     * @param newPassword      新密码
     */
    @Transactional
    @CacheEvict(value = "userConfig", key = "#userId")
    public void changePassword(Long userId, String originalPassword, String newPassword) {
        UserConfig config = userConfigMapper.findById(userId);
        if (config == null) {
            throw new BusinessException("用户不存在");
        }

        // 验证原密码
        if (!passwordEncoder.matches(originalPassword, config.getPassword())) {
            throw new BusinessException("原密码错误");
        }

        // 更新新密码
        config.setPassword(passwordEncoder.encode(newPassword));
        userConfigMapper.updateById(config);
    }
}
