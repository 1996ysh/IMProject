package com.shanyangcode.common.exception;

import com.shanyangcode.common.result.BaseResponse;
import com.shanyangcode.common.result.ErrorCode;
import com.shanyangcode.common.result.ResultUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * 全局异常处理器（公共版，通过 AutoConfiguration 对所有服务生效）
 * 各服务特有的异常可在本服务内单独写 @RestControllerAdvice 补充
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 业务异常（预期内，warn 级别，不打堆栈）
     */
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    /**
     * @RequestBody 参数校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<?> methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, firstErrorMessage(e.getBindingResult().getAllErrors()));
    }

    /**
     * 表单对象绑定校验失败
     */
    @ExceptionHandler(BindException.class)
    public BaseResponse<?> bindExceptionHandler(BindException e) {
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, firstErrorMessage(e.getBindingResult().getAllErrors()));
    }

    /**
     * @RequestParam/@PathVariable 参数校验失败
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public BaseResponse<?> constraintViolationExceptionHandler(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse(ErrorCode.PARAMS_ERROR.getMessage());
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, message);
    }

    /**
     * 缺少必填请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public BaseResponse<?> missingServletRequestParameterExceptionHandler(MissingServletRequestParameterException e) {
        log.warn("缺少必填参数: {}", e.getParameterName());
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, "缺少必填参数: " + e.getParameterName());
    }

    /**
     * 请求体 JSON 解析失败（格式错误/类型不匹配）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public BaseResponse<?> httpMessageNotReadableExceptionHandler(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, "请求体格式错误");
    }

    /**
     * 请求方法不支持（如用 GET 调 POST 接口）
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public BaseResponse<?> httpRequestMethodNotSupportedExceptionHandler(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMethod());
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, "不支持的请求方式: " + e.getMethod());
    }

    /**
     * 接口路径不存在（404）
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public BaseResponse<?> noResourceFoundExceptionHandler(NoResourceFoundException e) {
        log.warn("资源不存在: {}", e.getResourcePath());
        return ResultUtils.error(ErrorCode.NOT_FOUND_ERROR);
    }

    /**
     * 系统异常兜底（error 级别，打完整堆栈）
     */
    @ExceptionHandler(Exception.class)
    public BaseResponse<?> exceptionHandler(Exception e) {
        log.error("系统异常", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }

    private String firstErrorMessage(List<ObjectError> errors) {
        if (errors == null || errors.isEmpty()) {
            return ErrorCode.PARAMS_ERROR.getMessage();
        }
        String message = errors.get(0).getDefaultMessage();
        return message == null ? ErrorCode.PARAMS_ERROR.getMessage() : message;
    }
}
