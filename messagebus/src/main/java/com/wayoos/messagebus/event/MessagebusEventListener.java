package com.wayoos.messagebus.event;

/**
 * Created by steph on 24.08.16.
 */
public interface MessagebusEventListener {

    void onEvent(MessagebusEvent event);

}
