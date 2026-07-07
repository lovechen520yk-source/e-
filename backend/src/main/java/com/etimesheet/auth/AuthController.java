package com.etimesheet.auth;

import com.etimesheet.entity.UserConfig;
import com.etimesheet.repository.UserConfigMapper;
import com.etimesheet.util.ResponseResult;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证控制器（登录/注册/验证码）
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserConfigMapper userConfigMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    /** 滑块验证码存储：captchaKey -> {correctX, pieceY, pieceSize, timestamp} */
    private static final Map<String, SlideCaptchaStore> SLIDE_CAPTCHA_CACHE = new ConcurrentHashMap<>();
    /** 验证通过令牌存储：captchaToken -> timestamp */
    private static final Map<String, Long> CAPTCHA_TOKEN_CACHE = new ConcurrentHashMap<>();
    private static final long CAPTCHA_EXPIRE_MS = 5 * 60 * 1000L; // 5分钟
    private static final int POSITION_TOLERANCE = 5; // 拼图位置容差（像素）
    private static final int MAX_CAPTCHA_SIZE = 100; // 最大未验证验证码数量

    public AuthController(UserConfigMapper userConfigMapper, JwtUtil jwtUtil) {
        this.userConfigMapper = userConfigMapper;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * 获取滑块验证码
     */
    @GetMapping("/slide-captcha")
    public ResponseResult<?> getSlideCaptcha() {
        SlideCaptchaUtil.SlideCaptchaResult captcha = SlideCaptchaUtil.generate();
        String captchaKey = UUID.randomUUID().toString();

        // 只存储正确的位置X，不返回给前端
        // 限制缓存大小，防止内存溢出
        if (SLIDE_CAPTCHA_CACHE.size() >= MAX_CAPTCHA_SIZE) {
            // 超过上限时清理过期条目
            cleanExpired();
        }
        // 若清理后仍然超限，拒绝本次生成
        if (SLIDE_CAPTCHA_CACHE.size() >= MAX_CAPTCHA_SIZE) {
            return ResponseResult.error(429, "验证码请求过于频繁，请稍后重试");
        }
        SLIDE_CAPTCHA_CACHE.put(captchaKey, new SlideCaptchaStore(
                captcha.getPieceX(),
                captcha.getPieceY(),
                captcha.getPieceSize(),
                System.currentTimeMillis()
        ));

        // 清理过期验证码
        cleanExpired();

        return ResponseResult.success(Map.of(
                "captchaKey", captchaKey,
                "bgImage", captcha.getBgImage(),
                "pieceImage", captcha.getPieceImage(),
                "pieceY", captcha.getPieceY(),
                "pieceSize", captcha.getPieceSize()
        ));
    }

    /**
     * 验证滑块拼图位置
     */
    @PostMapping("/slide-captcha/verify")
    public ResponseResult<?> verifySlideCaptcha(@RequestBody SlideVerifyRequest request) {
        String captchaKey = request.getCaptchaKey();
        int positionX = request.getPositionX();

        if (captchaKey == null) {
            return ResponseResult.error(400, "参数错误");
        }

        SlideCaptchaStore store = SLIDE_CAPTCHA_CACHE.remove(captchaKey);
        if (store == null) {
            return ResponseResult.error(400, "验证码已过期，请刷新");
        }

        // 检查位置是否在容差范围内
        if (Math.abs(positionX - store.correctX) > POSITION_TOLERANCE) {
            return ResponseResult.error(400, "验证失败，请重试");
        }

        // 生成一次性验证通过令牌
        String captchaToken = UUID.randomUUID().toString();
        CAPTCHA_TOKEN_CACHE.put(captchaToken, System.currentTimeMillis());

        return ResponseResult.success(Map.of("captchaToken", captchaToken));
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseResult<?> register(@RequestBody RegisterRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();
        String captchaToken = request.getCaptchaToken();

        // 验证滑块验证码
        if (captchaToken == null || !validateCaptchaToken(captchaToken)) {
            return ResponseResult.error(400, "请完成人机验证");
        }

        if (username == null || username.trim().isEmpty()) {
            return ResponseResult.error(400, "用户名不能为空");
        }
        if (password == null || password.length() < 6) {
            return ResponseResult.error(400, "密码长度不能少于6位");
        }

        // 检查用户名是否已存在
        UserConfig existing = userConfigMapper.findByUsername(username.trim());
        if (existing != null) {
            return ResponseResult.error(400, "用户名已存在");
        }

        // 创建新用户
        UserConfig user = new UserConfig();
        user.setUsername(username.trim());
        user.setPassword(passwordEncoder.encode(password));
        user.setCompany("");
        user.setDepartment("");
        user.setPosition("");
        user.setProject("");
        user.setContent("");
        user.setSalaryMode("FIXED");

        userConfigMapper.insert(user);

        // 生成token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        return ResponseResult.success("注册成功", Map.of(
                "token", token,
                "username", user.getUsername(),
                "name", user.getName() != null ? user.getName() : ""
        ));
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseResult<?> login(@RequestBody LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();
        String captchaToken = request.getCaptchaToken();

        // 验证滑块验证码
        if (captchaToken == null || !validateCaptchaToken(captchaToken)) {
            return ResponseResult.error(400, "请完成人机验证");
        }

        // 验证参数
        if (username == null || username.trim().isEmpty()) {
            return ResponseResult.error(400, "用户名不能为空");
        }
        if (password == null || password.isEmpty()) {
            return ResponseResult.error(400, "密码不能为空");
        }

        // 查询用户
        UserConfig user = userConfigMapper.findByUsername(username.trim());
        if (user == null) {
            return ResponseResult.error(400, "用户名或密码错误");
        }

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseResult.error(400, "用户名或密码错误");
        }

        // 生成token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        return ResponseResult.success(Map.of(
                "token", token,
                "username", user.getUsername(),
                "name", user.getName() != null ? user.getName() : ""
        ));
    }

    /**
     * 验证 captchaToken 是否有效
     */
    private boolean validateCaptchaToken(String captchaToken) {
        Long timestamp = CAPTCHA_TOKEN_CACHE.remove(captchaToken);
        if (timestamp == null) {
            return false;
        }
        // 检查是否过期
        if (System.currentTimeMillis() - timestamp > CAPTCHA_EXPIRE_MS) {
            return false;
        }
        return true;
    }

    /**
     * 清理过期的缓存
     */
    private void cleanExpired() {
        long now = System.currentTimeMillis();
        SLIDE_CAPTCHA_CACHE.values().removeIf(s -> (now - s.timestamp) > CAPTCHA_EXPIRE_MS);
        CAPTCHA_TOKEN_CACHE.values().removeIf(t -> (now - t) > CAPTCHA_EXPIRE_MS);
    }

    /** 滑块验证码存储 */
    private static class SlideCaptchaStore {
        final int correctX;
        final int pieceY;
        final int pieceSize;
        final long timestamp;

        SlideCaptchaStore(int correctX, int pieceY, int pieceSize, long timestamp) {
            this.correctX = correctX;
            this.pieceY = pieceY;
            this.pieceSize = pieceSize;
            this.timestamp = timestamp;
        }
    }

    /** 滑块验证请求体 */
    public static class SlideVerifyRequest {
        private String captchaKey;
        private int positionX;

        public String getCaptchaKey() { return captchaKey; }
        public void setCaptchaKey(String captchaKey) { this.captchaKey = captchaKey; }
        public int getPositionX() { return positionX; }
        public void setPositionX(int positionX) { this.positionX = positionX; }
    }

    /** 登录请求体 */
    public static class LoginRequest {
        private String username;
        private String password;
        private String captchaToken;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getCaptchaToken() { return captchaToken; }
        public void setCaptchaToken(String captchaToken) { this.captchaToken = captchaToken; }
    }

    /** 注册请求体 */
    public static class RegisterRequest {
        private String username;
        private String password;
        private String captchaToken;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getCaptchaToken() { return captchaToken; }
        public void setCaptchaToken(String captchaToken) { this.captchaToken = captchaToken; }
    }
}
