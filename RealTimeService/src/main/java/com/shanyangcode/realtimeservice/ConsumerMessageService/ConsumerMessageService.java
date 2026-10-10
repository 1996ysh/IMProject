package com.shanyangcode.realtimeservice.ConsumerMessageService;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;

import com.shanyangcode.Utils.FormatDateUtil;
import com.shanyangcode.common.constant.SessionTypeConstant;
import com.shanyangcode.model.dto.MessageRequest;
import com.shanyangcode.model.vo.MessageResponse;
import com.shanyangcode.realtimeservice.websocket.ChannelManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.net.http.WebSocket;

/**
 *消费消息，然后通过websocket推送给在线用户
 */
@Service
@Slf4j
public class ConsumerMessageService {
    //监听message-topic
    @KafkaListener(topics = "message-topic", groupId = "infinite-chat-push-group")
    public void consume(String message) {
        System.out.println("收到消息：" + message);
        MessageRequest messageRequest = JSONUtil.toBean(message, MessageRequest.class);
        System.out.println("收到消息：" + messageRequest);
        if (messageRequest.getSessionType() == SessionTypeConstant.SIGNAL_TYPE) {
            //如果是单聊
            signalMessage(messageRequest);
        } else if (messageRequest.getSessionType() == SessionTypeConstant.GROUP_TYPE) {
            //如果是群聊
            groupMessage(messageRequest);
        }
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
        // todo
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