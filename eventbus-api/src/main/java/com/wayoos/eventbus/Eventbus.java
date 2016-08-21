package com.wayoos.eventbus;

/**
 * Created by steph on 21.08.16.
 */
public class Eventbus {

    public <T> Channel getChannel(String name, Class<T> messageClass) {
        return new Channel();
    }

}
