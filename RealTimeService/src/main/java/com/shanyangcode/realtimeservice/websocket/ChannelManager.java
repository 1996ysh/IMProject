package com.shanyangcode.realtimeservice.websocket;

import io.netty.channel.Channel;

import java.util.concurrent.ConcurrentHashMap;

public class ChannelManager {
    /**
     * 这里面设置两个map的原因是因为推消息和离线的时候，得到的东西不一样，
     * 推消息的时候可以用userId，但是离线的时候只有channel
     */
    private static final ConcurrentHashMap<String, Channel> USER_CHANNEL_MAP = new ConcurrentHashMap<String,Channel>();
    private static final ConcurrentHashMap<Channel, String> CHANNEL_USER_MAP = new ConcurrentHashMap<Channel, String>();
    public static void addUserChannel(Channel channel, String userId) {
        USER_CHANNEL_MAP.put(userId, channel);
    }
    public static void addChannelUser(Channel channel, String userId) {
        CHANNEL_USER_MAP.put(channel, userId);
    }
    public static void removeChannelUser(Channel channel) {
        CHANNEL_USER_MAP.remove(channel);
    }

    public static void removeUserChannel(String userId) {
        USER_CHANNEL_MAP.remove(userId);
    }

    public static Channel getChannelByUserId(String userId) {
        return USER_CHANNEL_MAP.get(userId);
    }

    public static String getUserIdByChannel(Channel channel) {
        return CHANNEL_USER_MAP.get(channel);
    }
}
