package com.wo.api.client;

import com.wo.api.client.fallback.UserClientFallback;
import com.wo.api.config.InternalFeignConfig;
import com.wo.api.dto.user.UserInfo;
import com.wo.api.dto.user.UserVO;
import com.wo.common.result.PageResult;
import com.wo.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "wo-service-user", fallbackFactory = UserClientFallback.class, configuration = InternalFeignConfig.class)
public interface UserClient {

    @GetMapping("/api/users/{id}")
    R<UserInfo> getUserInfo(@PathVariable("id") Long id);

    @GetMapping("/api/users")
    R<PageResult<UserVO>> listUsers(@RequestParam(value = "role", required = false) String role,
                                    @RequestParam(value = "department", required = false) String department);

    @GetMapping("/api/users/by-username/{username}")
    R<UserVO> getUserByUsername(@PathVariable("username") String username);
}
