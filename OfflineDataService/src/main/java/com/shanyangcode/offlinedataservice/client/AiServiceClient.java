package com.shanyangcode.offlinedataservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
//feignClient这个注解的意思是AiService这个包下面controller去实现具体逻辑，这里只是声明
@FeignClient(name = "AiService")
public interface AiServiceClient {

    @GetMapping("/api/ai/summary")
    String chatSummary(@RequestParam("historyLog") String historyLog);

}