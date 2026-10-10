package com.shanyangcode.realtimeservice.websocket;

import com.shanyangcode.Utils.JwtUtil;
import com.shanyangcode.common.constant.CommonConstant;
import io.jsonwebtoken.Claims;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

/**
 * 鉴权之后与用户进行绑定
 */
@RequiredArgsConstructor
public class WebSocketAuthHeader extends ChannelInboundHandlerAdapter {
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof FullHttpRequest request){
            String authHeader = request.headers().get("Authorization");
            if(authHeader == null || authHeader.isEmpty()){
                ctx.close();
                return;
            }
            try{
                Claims claims = JwtUtil.parse(authHeader);
                if(claims == null){
                    ctx.close();
                    return;
                }
                String userId = claims.getSubject();
                if(userId == null || userId.isEmpty()){
                    ctx.close();
                    return;
                }
                //取出token
                String storedToken = stringRedisTemplate.opsForValue().get(CommonConstant.ACCESS_TOKEN_PREFIX + userId);
                if(StringUtils.isEmpty(storedToken)|| !authHeader.equals(storedToken)){
                    ctx.close();
                    return;
                }
                //绑定用户与channel
                ChannelManager.addUserChannel(ctx.channel(),userId);
                ChannelManager.addChannelUser(ctx.channel(),userId);
                //放行消息
                ctx.fireChannelRead(msg);
            }catch (Exception e){
                //记录日志
                ctx.close();
            }
        }else{
            ctx.fireChannelRead(msg);
        }


    }
}
