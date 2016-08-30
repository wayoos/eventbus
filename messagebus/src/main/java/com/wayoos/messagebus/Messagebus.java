package com.wayoos.messagebus;

import com.wayoos.messagebus.channel.Channel;
import com.wayoos.messagebus.event.MessageConsumedEventListener;
import com.wayoos.messagebus.event.MessageEventListenerRegistry;
import com.wayoos.messagebus.event.MessagePostedEventListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Created by steph on 21.08.16.
 */
public class Messagebus {

    private final MessagebusExecutorFactory messagebusExecutorFactory;

    private final MessageEventListenerRegistry messageEventListenerRegistry = new MessageEventListenerRegistry();

    private Map<String, Channel> channels = new ConcurrentHashMap<>();


    public Messagebus() {
        this(() -> Executors.newCachedThreadPool());
    }

    public Messagebus(MessagebusExecutorFactory messagebusExecutorFactory) {
        this.messagebusExecutorFactory = messagebusExecutorFactory;
    }

    public Messagebus with(MessagePostedEventListener listener) {
        messageEventListenerRegistry.add(listener);
        return this;
    }

    public Messagebus with(MessageConsumedEventListener listener) {
        messageEventListenerRegistry.add(listener);
        return this;
    }

    public <T> Channel<T> createChannel(String alias, Class<T> messageType) {
        Channel<T> channel = new Channel<T>(alias, messagebusExecutorFactory, messageType, messageEventListenerRegistry);

        Channel<T> newChannel = channels.putIfAbsent(alias, channel);
        if (newChannel == null) {
            return channel;
        } else {
            return newChannel;
        }
    }

    /**
     * Retrieves the Channel associated with the given alias, if one is known.
     * @param alias - the alias under which to look the Channel up
     * @param messageType - the Channel message class
     * @return the Channel associated with the given alias, null if no such channel exists
     * @throws IllegalArgumentException - if the messageType do not match the ones with which the Channel was created
     */
    public <T> Channel<T> getChannel(String alias, Class<T> messageType) throws IllegalArgumentException {
        Channel<T> channel = channels.get(alias);
        if (channel != null) {
            if (!channel.messageType().equals(messageType))
                throw new IllegalArgumentException("Invalid message type.");
        }
        return channel;
    }

}
