package com.lyq.kb.config;

import com.lyq.kb.common.ForbiddenException;
import com.lyq.kb.common.RateLimitException;
import com.lyq.kb.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：所有Controller抛出的异常都先到这里，
 * 统一包装成Result返回，Controller和Service里就不用到处try-catch
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务错误（用户名已存在、密码错误等）：400 + 错误信息 */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleBiz(IllegalArgumentException e) {
        return Result.fail(400, e.getMessage());
    }

    /** @Valid参数校验失败：400 + 第一条错误提示 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return Result.fail(400, msg);
    }

    /** 其他没预料到的异常：500，日志记录完整堆栈方便排查 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnknown(Exception e) {
        log.error("未处理异常", e);
        return Result.fail(500, "服务器内部错误");
    }
    /** 权限不足：403 */
    @ExceptionHandler(ForbiddenException.class)
    public Result<Void> handleForbidden(ForbiddenException e) {
        return Result.fail(403, e.getMessage());
    }

    /**
     * 限流：真HTTP 429（不能只body里写code=429）——
     * SSE流式接口被限时，前端streamPost靠HTTP状态码判断失败，
     * 返回200会被当成正常流去读，用户看不到任何提示
     */
    @ExceptionHandler(RateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Result<Void> handleRateLimit(RateLimitException e) {
        return Result.fail(429, e.getMessage());
    }
}