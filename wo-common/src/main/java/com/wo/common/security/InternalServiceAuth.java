package com.wo.common.security;

import com.wo.common.constant.CommonConstant;
import com.wo.common.enums.ErrorCode;
import com.wo.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class InternalServiceAuth {

    @Value("${wo.internal-service-token:wo-internal-dev-token}")
    private String internalServiceToken;

    public void require(HttpServletRequest request) {
        if (!isInternalRequest(request)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅允许内部服务调用");
        }
    }

    public boolean isInternalRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String token = request.getHeader(CommonConstant.INTERNAL_SERVICE_TOKEN_HEADER);
        return StringUtils.hasText(token)
                && StringUtils.hasText(internalServiceToken)
                && internalServiceToken.equals(token);
    }
}
