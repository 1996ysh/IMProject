package com.shanyangcode.common.constant;

import java.util.concurrent.TimeUnit;

public class CommonConstant {

    public static final String TOKEN_SECRET_KEY = "shanyangcodebackendInfiniteChatshanyangcodebackendInfiniteChat";

    public static final Integer ACCESS_TOKEN_EXPIRE_TIME =  30;

    public static final Integer REFRESH_TOKEN_EXPIRE_TIME =  7;

    public static final String ACCESS_TOKEN_PREFIX = "access:token:";

    public static final String REFRESH_TOKEN_PREFIX = "refresh:token:";

    public static final TimeUnit ACCESS_TOKEN_UNIT = TimeUnit.MINUTES; // 分钟

    public static final TimeUnit REFRESH_TOKEN_UNIT = TimeUnit.DAYS; // 天

    public static final String KAFKA_MESSAGE_TOPIC_STORE = "store-topic";

    public static final String KAFKA_MESSAGE_TOPIC_PUSH = "message-topic";

    public static final String REDIS_NETTY_URI = "nettyUri";
    //nacos中服务的名字
    public static final String DISCOVERY_CLIENT_NAME = "RealTimeService";
    //netty前缀url
    public static final String NETTY_SERVICE_URI = "/ws/netty";
    //minIO中的桶名
    public static final String BUCKET_NAME = "infinitechat";
    //过期时间
    public static final Integer PICTURE_EXPIRE_TIME = 3000;
}