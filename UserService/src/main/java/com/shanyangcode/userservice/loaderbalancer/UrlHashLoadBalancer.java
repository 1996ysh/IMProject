package com.shanyangcode.userservice.loaderbalancer;


import org.springframework.cloud.client.ServiceInstance;

import java.util.HashMap;
import java.util.List;

/**
 * 通过userid哈希负载均衡来选定连接哪一台服务实例
 */
public class UrlHashLoadBalancer {


    public ServiceInstance select(List<ServiceInstance> instances, String userId) {
        ConsistentHash consistentHash = new ConsistentHash(instances);
        HashMap<String, ServiceInstance> map = consistentHash.map;
        return map.get(consistentHash.getServer(userId));
    }
}