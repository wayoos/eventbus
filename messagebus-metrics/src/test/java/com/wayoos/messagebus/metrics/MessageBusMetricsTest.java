package com.wayoos.messagebus.metrics;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.wayoos.messagebus.Messagebus;
import com.wayoos.messagebus.RegisterType;
import com.wayoos.messagebus.channel.Channel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Executors;

import static org.junit.Assert.*;

/**
 * Created by steph on 29.08.16.
 */
public class MessageBusMetricsTest {

    Messagebus messagebus;

    MetricRegistry metricRegistry = new MetricRegistry();

    @Before
    public void setUp() throws Exception {
        messagebus = new Messagebus(() -> Executors.newCachedThreadPool(),
                new MessageBusMetrics(metricRegistry));
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void test() {
        Channel<String> channel1 = messagebus.createChannel("test", String.class);
        Channel<String> channel2 = messagebus.createChannel("test2", String.class);

        channel1.register(s -> {}, RegisterType.SYNC);


        for (int i=1; i<=1000; i++) {
            channel1.post("msg"+i);
            channel2.post("msg"+i);
        }

        ConsoleReporter.forRegistry(metricRegistry).build().report();
    }

}