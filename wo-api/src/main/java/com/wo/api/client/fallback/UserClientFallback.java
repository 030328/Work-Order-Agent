package com.wo.api.client.fallback;

import com.wo.api.client.UserClient;
import com.wo.api.dto.user.UserInfo;
import com.wo.api.dto.user.UserVO;
import com.wo.common.result.PageResult;
import com.wo.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserClientFallback implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(Throwable cause) {
        log.error("UserClient fallback triggered", cause);
        return new UserClient() {

            @Override
            public R<UserInfo> getUserInfo(Long id) {
                return R.fail("用户服务不可用，请稍后重试");
            }

            @Override
            public R<PageResult<UserVO>> listUsers(String role, String department) {
                return R.fail("用户服务不可用，请稍后重试");
            }

            @Override
            public R<UserVO> getUserByUsername(String username) {
                return R.fail("用户服务不可用，请稍后重试");
            }
        };
    }
}
