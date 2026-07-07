package com.etimesheet.controller;

import com.etimesheet.entity.UserConfig;
import com.etimesheet.service.UserConfigService;
import com.etimesheet.util.ResponseResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户配置 REST API 控制器
 */
@RestController
@RequestMapping("/api/user")
public class UserConfigController {

    private final UserConfigService userConfigService;

    public UserConfigController(UserConfigService userConfigService) {
        this.userConfigService = userConfigService;
    }

    /**
     * 获取当前登录用户的配置
     */
    @GetMapping("/config")
    public ResponseResult<UserConfig> getConfig(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        UserConfig config = userConfigService.getConfig(userId);
        // 返回时屏蔽密码字段
        config.setPassword(null);
        return ResponseResult.success(config);
    }

    /**
     * 保存/更新当前登录用户的配置
     */
    @PostMapping("/config")
    public ResponseResult<UserConfig> saveConfig(HttpServletRequest request, @RequestBody UserConfig config) {
        Long userId = (Long) request.getAttribute("userId");
        config.setId(userId);
        UserConfig saved = userConfigService.saveConfig(config);
        saved.setPassword(null);
        return ResponseResult.success(saved);
    }

    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public ResponseResult<?> changePassword(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        String originalPassword = body.get("originalPassword");
        String newPassword = body.get("newPassword");

        if (originalPassword == null || originalPassword.isEmpty()) {
            return ResponseResult.error(400, "请输入原密码");
        }
        if (newPassword == null || newPassword.length() < 6) {
            return ResponseResult.error(400, "新密码长度不能少于6位");
        }

        try {
            userConfigService.changePassword(userId, originalPassword, newPassword);
            return ResponseResult.success("密码修改成功", null);
        } catch (Exception e) {
            return ResponseResult.error(400, e.getMessage());
        }
    }
}
