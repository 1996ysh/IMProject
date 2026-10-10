package com.shanyangcode.realtimeservice.websocket;

import cn.hutool.json.JSONUtil;
import com.shanyangcode.common.constant.CommonConstant;
import com.shanyangcode.model.dto.MessageRequest;
import com.shanyangcode.realtimeservice.Utils.SnowflakeDynamicUtil;
import com.shanyangcode.realtimeservice.constants.WebSocketConstant;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.timeout.IdleStateEvent;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Date;

@Slf4j
@AllArgsConstructor
public class WebSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private final KafkaTemplate<String, String> kafkaTemplate;

    private final SnowflakeDynamicUtil snowflake;
    //3.收到websocket消息
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame textWebSocketFrame) throws Exception {
        Channel channel = channelHandlerContext.channel();
        String msg = textWebSocketFrame.text();
        try{
            if(WebSocketConstant.HEARTBEAT_PING.equals(msg)){
                //心跳响应
                if(channel.isActive()){
                    log.debug("received heartbeat ping from {}",channel.id());
                    channel.writeAndFlush(WebSocketConstant.HEARTBEAT_PONG);
                }
            }else {

                if(channel.isActive()){
                    // 业务消息 → 发 Kafka
                    sendMessageKafka(msg, channel);
                }else{
                    log.warn("channel is inactive");
                }

            }
        }catch (Exception e){
            log.error("Error  handling message from {}:{}",channel.id(),msg,e);
            clearChannel(channel);
            channelHandlerContext.close();
        }
    }
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {

        // 处理心跳
        if (evt instanceof IdleStateEvent event) {
            switch (event.state()) {
                case READER_IDLE:
                    log.error("读空闲超时");
                    clearChannel(ctx.channel());
                    ctx.close();
                    break;
                case WRITER_IDLE:
                    log.error("写空闲超时");
                case ALL_IDLE:
                    log.error("读写空闲超时");
            }
        }
    }

    public void sendMessageKafka(String message, Channel channel) {
        MessageRequest messageRequest = JSONUtil.toBean(message, MessageRequest.class);
        messageRequest.setMessageId(snowflake.nextId());
        messageRequest.setCreatedTime(new Date());

        // 消息存储, 存储只存储一次，避免重复消费
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

        // 消息推送消息
        kafkaTemplate.send(CommonConstant.KAFKA_MESSAGE_TOPIC_PUSH, messageRequest.getSessionId().toString(), JSONUtil.toJsonStr(messageRequest)).whenComplete((success, failure) -> {
            if (failure != null) {
                // 生产者生产失败
                System.err.println("生产者生产失败: " + failure.getMessage());
                // 记录日志、告警、补偿等
            } else {
                // 生产者生产成功
                System.out.println("生产者生产成功消息推送，offset: " + success.getRecordMetadata().offset());
            }
        });
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Exception Caught in channel pipeline", cause);
    }
    //2.连接建立完成
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        log.info("Channel active in channel pipeline");
    }
    //4.连接断开
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        clearChannel(ctx.channel());
        super.channelInactive(ctx);
        System.out.println("channel inActive");
    }
    // 1.处理器被添加到channelPipeline
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);
        System.out.println("Handler added in channel pipeline");
    }
    //5.处理器从channelPipe移除
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        super.handlerRemoved(ctx);
        System.out.println("Handler removed in channel pipeline");
    }
    public void clearChannel(Channel channel) {
        if (channel == null) {
            return;
        }
        System.out.println("clearChannel: " + channel.id());

        String userId = ChannelManager.getUserIdByChannel(channel);
        try {
            if (userId != null) {
                ChannelManager.removeUserChannel(userId);
            }
            ChannelManager.removeChannelUser(channel);
        } catch (Exception e) {
            log.error("clearChannel failed for channel: {}, userId: {}", channel.id(), userId, e);
        } finally {
            if (channel.isActive()) {
                channel.close();
            }
        }
    }
}
