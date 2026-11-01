package com.medistock.common.exception;

import com.medistock.common.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    /** 验证：业务异常转 Result，且 HTTP 状态码随 code 映射（业务码 1000+ → 400） */
    @Test
    void businessException_mapsToResult() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<Result<Void>> response = handler.handleBusiness(
                new BusinessException(ErrorCode.ORDER_STATUS_INVALID));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Result<Void> result = response.getBody();
        assertThat(result.getCode()).isEqualTo(1007);
        assertThat(result.getMessage()).isEqualTo("单据状态不允许该操作");
    }

    /** 验证：404 类业务异常（资源不存在）HTTP 状态码为 404，而非笼统的 500 */
    @Test
    void notFound_mapsToHttp404() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<Result<Void>> response = handler.handleBusiness(
                new BusinessException(ErrorCode.NOT_FOUND));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getCode()).isEqualTo(404);
    }
}
