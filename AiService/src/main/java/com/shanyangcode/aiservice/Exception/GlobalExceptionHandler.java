package com.shanyangcode.aiservice.Exception;

import com.shanyangcode.common.result.BaseResponse;
import com.shanyangcode.common.result.ErrorCode;
import com.shanyangcode.common.result.ResultUtils;
import dev.langchain4j.guardrail.InputGuardrailException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * AiService 特有的异常处理器
 * 通用异常（业务异常、参数校验、系统异常等）由 Common 模块的全局异常处理器统一处理
 */
@RestControllerAdvice
@Order(1)
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 敏感词拦截（AI 输入护栏）
     */
    @ExceptionHandler(InputGuardrailException.class)
    public BaseResponse<?> inputGuardrailExceptionHandler(InputGuardrailException e) {
        log.warn("敏感词拦截: {}", e.getMessage());
        return ResultUtils.error(ErrorCode.SENSITIVE_WORD_ERROR);
    }
}
