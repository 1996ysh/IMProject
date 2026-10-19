package com.shanyangcode.offlinedataservice.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 定义feign rpc接口
 */
@FeignClient(name = "UserService")
public interface UserServiceClient {

    @GetMapping("/api/user/get/sessions")
    List<Long> getSessionIdsByUserId(@RequestParam("userId")  Long userId);
    //获取昵称
    @GetMapping("/api/user/get/nickname")
    Map<Long, String> getUserNickName(@RequestParam("sessionId")  Long sessionId);
}