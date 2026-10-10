package com.shanyangcode.realtimeservice.Utils;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SnowflakeDynamicUtil {

    @Value("${snowflake.workerId}")
    private long workerId;

    @Value("${snowflake.datacenterId}")
    private long dataCenterId;

    private Snowflake snowflake;

    // Bean创建完成后初始化雪花对象
    @PostConstruct
    public void init() {
        snowflake = IdUtil.getSnowflake(workerId, dataCenterId);
    }

    public long nextId() {
        return snowflake.nextId();
    }
}
