package com.wayoos.messagebus.event;

import com.wayoos.messagebus.RegisterType;

import java.util.function.Consumer;

/**
 * Created by steph on 24.08.16.
 */
public interface MessagebusEvent {

    String getChannelAlias();

    RegisterType getRegisterType();

    Consumer getConsumer();

    EventType getEventType();

    long getDuration();

}
