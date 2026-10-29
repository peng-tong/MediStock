package com.medistock.common.exception;

import com.medistock.common.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    /** 验证：直接调用 handler，BusinessException 被转为 code=1007 的 Result */
    @Test
    void businessException_mapsToResult() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Result<Void> result = handler.handleBusiness(
                new BusinessException(ErrorCode.ORDER_STATUS_INVALID));
        assertThat(result.getCode()).isEqualTo(1007);
        assertThat(result.getMessage()).isEqualTo("单据状态不允许该操作");
    }
}
