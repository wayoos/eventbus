package com.wayoos.messagebus.event;

import com.wayoos.messagebus.RegisterType;

/**
 * Created by steph on 24.08.16.
 */
public interface MessagebusEvent {

    String getChannelId();

    Object getMessage();

    RegisterType getRegisterType();

    EventType getEventType();

}
