package com.wayoos.messagebus.metrics;

import com.codahale.metrics.MetricRegistry;
import com.wayoos.messagebus.Messagebus;

import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * Created by steph on 29.08.16.
 */
public class MessageBusMetrics {

    private final MetricRegistry metricRegistry;

    private static final String METRIC_PREFIX = MessageBusMetrics.class.getPackage().getName();

    private static final String CONSUMED = "consumed";

    private MessageBusMetrics(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    public static MessageBusMetrics with(MetricRegistry metricRegistry) {
        return new MessageBusMetrics(metricRegistry);
    }

    public MessageBusMetrics register(Messagebus messagebus) {
        messagebus.with(this::onPostedEvent);
        messagebus.with(this::onConsumedEvent);
        return this;
    }

    private void onPostedEvent(String alias) {
        // Global metrics
        metricRegistry.meter(name(METRIC_PREFIX, "posted")).mark();

        // Channel metrics
        metricRegistry.meter(name(METRIC_PREFIX, alias, "posted")).mark();
    }

    private void onConsumedEvent(String alias, String consumerAlias, long duration) {
        // Global metrics
        metricRegistry.timer(name(METRIC_PREFIX, CONSUMED)).update(duration, TimeUnit.MILLISECONDS);

        // Channel metrics
        metricRegistry.timer(name(METRIC_PREFIX, alias, CONSUMED)).update(duration, TimeUnit.MILLISECONDS);

        // Channel consumer metrics
        metricRegistry.timer(name(METRIC_PREFIX, alias, consumerAlias,
                CONSUMED)).update(duration, TimeUnit.MILLISECONDS);

    }

}
