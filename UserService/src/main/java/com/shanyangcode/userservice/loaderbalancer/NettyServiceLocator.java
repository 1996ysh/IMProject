package com.shanyangcode.userservice.loaderbalancer;

import com.shanyangcode.common.constant.CommonConstant;
import jakarta.annotation.Resource;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 获取realtime nacos实例，然后通过urlhashloadbalancer.select去筛选  最后拼成一个url返回
 */
@Service
public class NettyServiceLocator {


    @Resource
    private DiscoveryClient discoveryClient;


    public String getServiceInstance(String userId){
        List<ServiceInstance> instances = discoveryClient.getInstances(CommonConstant.DISCOVERY_CLIENT_NAME);
        if (instances.isEmpty()) {
            return null;
        }
        //使用hash来负载均衡一个节点给客户端使用
        ServiceInstance instance = new UrlHashLoadBalancer().select(instances, userId);
        //host:port/ws/netty
        return instance.getHost() + ":" + instance.getPort() + CommonConstant.NETTY_SERVICE_URI;
    }
}