package com.wo.common.constant;

public class RedisKeyConstant {

    public static final String USER_TOKEN = "user:token:";
    public static final String USER_INFO = "user:info:";
    public static final String AI_SESSION = "ai:session:";
    public static final String AI_SESSION_META = "ai:session:%s:meta";
    public static final String RATE_LIMIT = "rate:limit:";
    public static final String DISTRIBUTED_LOCK = "lock:";
    public static final String SLA_CHECK = "sla:check:";

    private RedisKeyConstant() {}
}
