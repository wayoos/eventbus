package com.wayoos.messagebus.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by steph on 30.08.16.
 */
public class MessagebusEventListenerRegistry {

    private final List<MessagebusPostedEventListener> postedEventListeners = new CopyOnWriteArrayList<>();
    private final List<MessagebusConsumedEventListener> consumedEventListeners = new CopyOnWriteArrayList<>();

    public void add(MessagebusPostedEventListener listener) {
        postedEventListeners.add(listener);
    }

    public void add(MessagebusConsumedEventListener listener) {
        consumedEventListeners.add(listener);
    }

    public void notifyPostedMessage(String channelAlias) {
        postedEventListeners.forEach(listener -> sentEventSilently(() -> listener.onEvent(channelAlias)));
    }

    public void notifyConsumedMessage(String channelAlias, String consumerAlias, long duration) {
        consumedEventListeners.forEach(listener -> sentEventSilently(() -> listener.onEvent(channelAlias, consumerAlias, duration)));
    }

    private void sentEventSilently(Runnable notify) {
        try {
            notify.run();
        } catch (Exception e) {
            // no exception traced. It's the responsability of listener implementation
        }
    }

}