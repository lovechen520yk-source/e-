package com.etimesheet.config;

import com.etimesheet.util.BusinessException;
import com.etimesheet.util.ResponseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * <p>
 * 将未捕获的 BusinessException、参数错误等统一转换为 {@link ResponseResult} 格式返回，
 * 避免客户端收到原始 HTTP 500 或不一致的响应结构。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常 -> 400
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseResult<?> handleBusinessException(BusinessException e) {
        return ResponseResult.error(400, e.getMessage());
    }

    /**
     * 参数缺失 -> 400
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseResult<?> handleMissingParam(MissingServletRequestParameterException e) {
        return ResponseResult.error(400, "缺少必要参数：" + e.getParameterName());
    }

    /**
     * 非法参数 -> 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseResult<?> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseResult.error(400, e.getMessage());
    }

    /**
     * 兜底异常 -> 500
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseResult<?> handleException(Exception e) {
        log.error("服务器内部错误", e);
        return ResponseResult.error(500, "服务器内部错误，请稍后重试");
    }
}
