package com.wayoos.messagebus.event;

/**
 * Created by steph on 30.08.16.
 */
public interface MessagebusConsumedEventListener {

    /**
     * this method is called after a message is consumed by a Consumer.
     * @param channelAlias
     * @param duration - duration in millisecond of message accepted by consumer.
     */
    void onEvent(String channelAlias, String consumerAlias, long duration);

}
