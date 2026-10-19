package com.shanyangcode.realtimeservice.ConsumerMessageService;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;

import com.shanyangcode.Utils.FormatDateUtil;
import com.shanyangcode.common.constant.CommonConstant;
import com.shanyangcode.common.constant.SessionTypeConstant;
import com.shanyangcode.model.dto.ChatRequest;
import com.shanyangcode.model.dto.MessageBody;
import com.shanyangcode.model.dto.MessageRequest;
import com.shanyangcode.model.vo.MessageResponse;
import com.shanyangcode.realtimeservice.client.AiServiceClient;
import com.shanyangcode.realtimeservice.client.UserServiceClient;
import com.shanyangcode.realtimeservice.websocket.ChannelManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 *消费消息，然后通过websocket推送给在线用户
 */
@Service
@Slf4j
public class ConsumerMessageService {
    @Value("${snowflake.datacenterId}")
    private Long datacenterId;
    @Value("${snowflake.workerId}")
    private Long workerId;
    @Resource
    private UserServiceClient userServiceClient;
    @Resource
    private AiServiceClient aiServiceClient;

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;
    //监听message-topic  实现发送消息
    @KafkaListener(topics = "message-topic", groupId = "infinite-chat-push-group")
    public void consume(String message) {
        System.out.println("收到消息：" + message);
        //一个json格式的message字符串转化为messageRequest对象
        MessageRequest messageRequest = JSONUtil.toBean(message, MessageRequest.class);
        System.out.println("收到消息：" + messageRequest);
        if (messageRequest.getSessionType() == SessionTypeConstant.SIGNAL_TYPE) {
            //如果是单聊
            signalMessage(messageRequest);
        } else if (messageRequest.getSessionType() == SessionTypeConstant.GROUP_TYPE) {
            //如果是群聊
            groupMessage(messageRequest);
        }else if (messageRequest.getSessionType() == SessionTypeConstant.ROBOT_TYPE) {
            aiSignalMessage(messageRequest);
        }
    }

    /**
     * ai发送消息
     * @param messageRequest
     */
    public void aiSignalMessage(MessageRequest messageRequest) {
        Snowflake snowflake = IdUtil.getSnowflake(workerId,datacenterId);
        MessageResponse messageResponse = createMessageResponse(messageRequest);
        messageResponse.setMessageId(snowflake.nextId());
        // 获取 AI 回复
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setPrompt(messageRequest.getBody().getContent());
        chatRequest.setSessionId(messageRequest.getSessionId());
        chatRequest.setUserId(messageRequest.getSenderId());
        //rpc调用远程chat接口，让ai实现回答
        String chat = aiServiceClient.chat(chatRequest);

        MessageBody messageBody = messageResponse.getBody();
        messageBody.setContent(chat);
        messageResponse.setBody(messageBody);
        messageResponse.setSenderId(CommonConstant.AI_ID);

        pushMessageToUser(messageResponse, chatRequest.getUserId());
        BeanUtil.copyProperties(messageResponse,messageRequest);

        kafkaTemplate.send(CommonConstant.KAFKA_MESSAGE_TOPIC_STORE, JSONUtil.toJsonStr(messageRequest)).whenComplete((success, failure) -> {
            if (failure != null) {
                // 生产者生产失败
                System.err.println("生产者生产失败: " + failure.getMessage());
                // 记录日志、告警、补偿等
            } else {
                // 生产者生产成功
                System.out.println("生产者生产成功，offset: " + success.getRecordMetadata().offset());
            }
        });
    }

    //单聊消息推送
    //单聊需要推给两个人：发送者（自己能看到自己发的消息）和接收者。这样双方聊天界面都能实时显示新消息
    public void signalMessage(MessageRequest messageRequest) {
        //构造响应对象
        MessageResponse messageResponse = createMessageResponse(messageRequest);
        //推给发送者
        pushMessageToUser(messageResponse, messageRequest.getSenderId());
        //推给接收者
        pushMessageToUser(messageResponse, messageRequest.getReceiverId());
    }

    public void groupMessage(MessageRequest messageRequest) {
        //todo  这里的feign接口鉴权问题还没有解决 后续需要优化
        //获取到该会话所有应该接收的人的id
        List<Long>receiveUserIds=userServiceClient.getUserIdBySessionId(messageRequest.getSessionId());
        //构造响应对象
        MessageResponse messageResponse = createMessageResponse(messageRequest);
        receiveUserIds.forEach(receiveUserId->{
            pushMessageToUser(messageResponse, receiveUserId);
        });
    }

    public MessageResponse createMessageResponse(MessageRequest messageRequest) {
        MessageResponse messageResponse = new MessageResponse();
        BeanUtil.copyProperties(messageRequest, messageResponse);
        messageResponse.setCreatedTime(FormatDateUtil.formatDate(messageRequest.getCreatedTime()));
        return messageResponse;

    }
    //推送消息
    public void pushMessageToUser(MessageResponse messageResponse, Long receiverId) {
        Channel channel = ChannelManager.getChannelByUserId(receiverId.toString());
        //获取channel  看用户是否在线
        if (channel != null) {
            //把 MessageResponse 转成 JSON 字符串
            //包装成 TextWebSocketFrame（WebSocket 文本帧）
            TextWebSocketFrame frame = new TextWebSocketFrame(JSONUtil.toJsonStr(messageResponse));

            channel.writeAndFlush(frame).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.info("消息发送成功: {}", messageResponse);
                } else {
                    log.info("消息发送失败: {}", future.cause() != null ? future.cause().getMessage() : "未知错误");
                }
            });
        } else {
            log.info("channel 不存在");
        }
    }

}