package com.wayoos.messagebus.metrics;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.wayoos.messagebus.Messagebus;
import com.wayoos.messagebus.RegisterType;
import com.wayoos.messagebus.channel.Channel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;

/**
 * Created by steph on 29.08.16.
 */
public class MessageBusMetricsTest {

    Messagebus messagebus;

    MetricRegistry metricRegistry = new MetricRegistry();

    ExecutorService executorService;

    @Before
    public void setUp() throws Exception {
        messagebus = new Messagebus(() -> Executors.newCachedThreadPool());
        executorService = Executors.newCachedThreadPool();

        MessageBusMetrics.with(metricRegistry).register(messagebus);
    }

    @After
    public void tearDown() throws Exception {
        executorService.shutdown();
    }

    @Test
    public void test() {
        AtomicLong testReceivedMessageCounter = new AtomicLong();
        Channel<Long> channel1 = messagebus.createChannel("channel1", Long.class);
        Channel<Long> channel2 = messagebus.createChannel("channel2", Long.class);

        channel1.register(s -> {testReceivedMessageCounter.incrementAndGet(); sleep(s);}, RegisterType.SYNC);
        channel1.register(s -> {testReceivedMessageCounter.incrementAndGet(); sleep(s);}, RegisterType.SYNC);

        channel2.register(s -> {testReceivedMessageCounter.incrementAndGet(); sleep(s);}, RegisterType.SYNC);
        channel2.register(s -> {testReceivedMessageCounter.incrementAndGet(); sleep(s);}, RegisterType.ASYNC);
        channel2.register(s -> {testReceivedMessageCounter.incrementAndGet(); sleep(s);}, RegisterType.ASYNC_SERIAL);

        int numberTestMessage = 100;
        int numberReceivedMessage = numberTestMessage * (1*2 + 2*3);

        executorService.execute(() -> sendTestMessage(numberTestMessage, channel1));
        executorService.execute(() -> sendTestMessage(numberTestMessage, channel2));
        executorService.execute(() -> sendTestMessage(numberTestMessage, channel2));

        await().atMost(5, SECONDS).until(() -> testReceivedMessageCounter.get() == numberReceivedMessage);

        ConsoleReporter.forRegistry(metricRegistry).build().report();
    }

    private void sleep(long duration) {
        try {
            Thread.sleep(duration/100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // simulate exception
        if (duration%5==0) {
            throw new RuntimeException("Simulate exception");
        }
    }

    private void sendTestMessage(int nb, Channel<Long> channel) {
        for (int i=1; i<=nb; i++) {
            channel.post(new Long(i));
        }
    }

}