package com.wayoos.messagebus.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.wayoos.messagebus.event.EventType;
import com.wayoos.messagebus.event.MessagebusEvent;
import com.wayoos.messagebus.event.MessagebusEventListener;

import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * Created by steph on 29.08.16.
 */
public class MessageBusMetrics implements MessagebusEventListener {

    private final MetricRegistry metricRegistry;

    public MessageBusMetrics(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    @Override
    public void onEvent(MessagebusEvent event) {
        String name = "messagebus."+event.getEventType();

        metricRegistry.counter(name+".counter").inc();
        metricRegistry.meter(name+".meter").mark();

        String aliasName = "messagebus."+event.getChannelAlias();
        metricRegistry.counter(name("messagebus", event.getChannelAlias(), event.getConsumer()!=null?event.getConsumer().getClass().getSimpleName():null,
                "counter")).inc();
        metricRegistry.meter(aliasName+".meter").mark();

        if (EventType.BEFORE_CONSUME.equals(event.getEventType())) {
            Timer timer = metricRegistry.timer(name("messagebus", event.getChannelAlias()));
            timer.time();
        }

        if (EventType.AFTER_CONSUME.equals(event.getEventType())) {
            Timer timer = metricRegistry.timer(name("messagebus", event.getChannelAlias()));
            timer.update(event.getDuration(), TimeUnit.MILLISECONDS);
        }

    }

}
