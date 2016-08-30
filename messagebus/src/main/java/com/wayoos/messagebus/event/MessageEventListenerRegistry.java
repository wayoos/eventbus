package com.wayoos.messagebus.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by steph on 30.08.16.
 */
public class MessageEventListenerRegistry {

    private final List<MessagePostedEventListener> postedEventListeners = new CopyOnWriteArrayList<>();
    private final List<MessageConsumedEventListener> consumedEventListeners = new CopyOnWriteArrayList<>();

    public void add(MessagePostedEventListener listener) {
        postedEventListeners.add(listener);
    }

    public void add(MessageConsumedEventListener listener) {
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