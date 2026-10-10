package com.shanyangcode.realtimeservice.websocket;

import com.shanyangcode.realtimeservice.Utils.SnowflakeDynamicUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.NettyRuntime;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
//声明这是一个spring配置类

/**
 * 独立起一个 Netty 服务监听 9101 端口
 * 提供 ws://127.0.0.1:9101/ws/netty 这个 WebSocket 连接地址
 */
@Configuration
//所有被final修饰的变量都会被初始化
@RequiredArgsConstructor
public class NettyService {
    private final int port =9101;
    //前置用来接收请求，处理连接请求的线程组
    private final NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
    //用来工作的group，处理io操作的线程组  相当于这个才是work 默认取 CPU 核心数作为线程数量
    private final NioEventLoopGroup workerGroup = new NioEventLoopGroup(NettyRuntime.availableProcessors());
    private final StringRedisTemplate stringRedisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SnowflakeDynamicUtil snowflakeDynamicUtil;
    @PostConstruct
    public void start() throws InterruptedException {
        run();
    }
    public void run() throws InterruptedException {
        //声明服务线程
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        //设置线程组
        serverBootstrap.group(bossGroup, workerGroup)
                //使用Nio处理
                .channel(NioServerSocketChannel.class)
                //管道处理chain
                .childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline channelPipeline = socketChannel.pipeline();
//                      //按照顺序依次处理messages
                        channelPipeline.addLast(new HttpServerCodec());
                        channelPipeline.addLast(new HttpObjectAggregator(65536));
                        channelPipeline.addLast(new WebSocketAuthHeader(stringRedisTemplate));
                        channelPipeline.addLast(new WebSocketServerProtocolHandler("/ws/netty"));
                        channelPipeline.addLast(new IdleStateHandler(60, 0, 0));
                        //自定义pipeline
                        channelPipeline.addLast(new WebSocketHandler(kafkaTemplate,snowflakeDynamicUtil));
                    }
                });
        serverBootstrap.bind(port).sync();
    }
    @PreDestroy
    public void destroy() throws InterruptedException {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
