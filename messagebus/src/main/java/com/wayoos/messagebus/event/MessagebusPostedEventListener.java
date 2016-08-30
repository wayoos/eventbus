package com.wayoos.messagebus.event;

/**
 * Created by steph on 30.08.16.
 */
public interface MessagebusPostedEventListener {

    void onEvent(String channelAlias);

}
