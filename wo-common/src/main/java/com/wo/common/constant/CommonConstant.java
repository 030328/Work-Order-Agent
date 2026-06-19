package com.wo.common.constant;

public class CommonConstant {

    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String TOKEN_HEADER = "Authorization";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USERNAME_HEADER = "X-Username";
    public static final String ROLE_HEADER = "X-Role";

    public static final long TOKEN_EXPIRE_HOURS = 24;
    public static final long TOKEN_REFRESH_HOURS = 7 * 24;

    public static final int PAGE_DEFAULT = 1;
    public static final int PAGE_SIZE_DEFAULT = 10;
    public static final int PAGE_SIZE_MAX = 100;

    private CommonConstant() {}
}
