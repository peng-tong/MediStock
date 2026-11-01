package com.medistock.common.exception;

import com.medistock.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理：任何控制器抛出的异常都转为统一 Result，并携带真实 HTTP 状态码
 * （此前所有响应恒为 HTTP 200、查询不存在资源返回 code:500，语义不规范）
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        // code 落在 400~599 时直接作为 HTTP 状态码（404 资源不存在等）；
        // 业务错误码（1000+，如验证码错误/登录失败）按业务失败 400 返回
        HttpStatus status = (e.getCode() >= 400 && e.getCode() < 600)
                ? HttpStatus.resolve(e.getCode())
                : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status == null ? HttpStatus.BAD_REQUEST : status)
                .body(Result.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Result<Void>> handleValidation(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .orElse(ErrorCode.BAD_REQUEST.getMessage());
        return ResponseEntity.badRequest().body(Result.fail(ErrorCode.BAD_REQUEST.getCode(), msg));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Result<Void>> handleBadRequest(Exception e) {
        return ResponseEntity.badRequest().body(Result.fail(ErrorCode.BAD_REQUEST));
    }

    /** S4 修复：路径/查询参数类型不匹配或数字溢出（如 /api/users/abc、page=abc、pageSize 超长）→ 400 而非 500 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(Result.fail(ErrorCode.BAD_REQUEST));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> handleDuplicateKey(DuplicateKeyException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Result.fail(ErrorCode.VALIDATE_ERROR.getCode(), "数据已存在，请勿重复提交"));
    }

    /**
     * 请求方法不支持（如对仅有 PUT 映射的 /api/users/{id}/status 发 GET）：
     * 此前会落入 Exception 兜底返回 500，改为语义正确的 405
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Result.fail(HttpStatus.METHOD_NOT_ALLOWED.value(), "请求方法不支持"));
    }

    /** 访问不存在的接口/资源：返回 404 而非笼统的 500 系统内部错误 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNotFound(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.fail(ErrorCode.NOT_FOUND));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOther(Exception e) {
        log.error("未捕获异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ErrorCode.INTERNAL_ERROR));
    }
}
