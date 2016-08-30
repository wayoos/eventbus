package com.wayoos.messagebus.event;

/**
 * Created by steph on 30.08.16.
 */
public interface MessagePostedEventListener {

    void onEvent(String channelAlias);

}
