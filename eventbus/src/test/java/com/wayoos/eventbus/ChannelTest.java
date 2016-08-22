package com.wayoos.eventbus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

/**
 * Created by steph on 21.08.16.
 */
public class ChannelTest {

    Eventbus eventbus;
    ExecutorService executorService;

    @Before
    public void beforeTest() {
        executorService = Executors.newCachedThreadPool();
        eventbus = new Eventbus(() -> executorService);
    }

    @After
    public void afterTest() {
        executorService.shutdown();
        eventbus = null;
    }

    @Test
    public void getChannelSync() throws Exception {
        List<String> inputMessages = createMessages(5);
        List<String> processedMessages = Collections.synchronizedList(new ArrayList<>());

        test(RegisterType.SYNC, inputMessages, processedMessages);

        assertEquals(inputMessages, processedMessages);
    }

    @Test
    public void getChannelAsync() throws Exception {
        List<String> inputMessages = createMessages(100);
        List<String> processedMessages = Collections.synchronizedList(new ArrayList<>());

        test(RegisterType.ASYNC, inputMessages, processedMessages);

        assertEquals(new HashSet<>(inputMessages), new HashSet<>(processedMessages));
    }

    @Test
    public void getChannelAsyncSec() throws Exception {
        List<String> inputMessages = createMessages(100);
        List<String> processedMessages = Collections.synchronizedList(new ArrayList<>());

        test(RegisterType.ASYNC_SERIAL, inputMessages, processedMessages);

        assertEquals(inputMessages, processedMessages);
    }

    @Test
    public void getChannelAsyncSecWithAcceptError() throws Exception {
        List<String> inputMessages = createMessages(20);
        List<String> processedMessages = Collections.synchronizedList(new ArrayList<>());

        test(RegisterType.ASYNC_SERIAL, inputMessages, m -> {
            if (m.endsWith("0"))
                throw new RuntimeException("Error processing");
            processedMessages.add(m);
        });

        inputMessages.remove("msg10");
        inputMessages.remove("msg20");

        await().atMost(2, SECONDS).until(() -> inputMessages.size() == processedMessages.size());
        assertEquals(inputMessages, processedMessages);
    }

    @Test
    public void perfChannelTest() throws Exception {
        Channel<String> channel = eventbus.createChannel("perf", String.class);

        final AtomicLong syncCount = new AtomicLong();
        final AtomicLong asyncCount = new AtomicLong();
        final AtomicLong serialCount = new AtomicLong();

        channel.register(m -> syncCount.incrementAndGet(), RegisterType.SYNC);
        channel.register(m -> asyncCount.incrementAndGet(), RegisterType.ASYNC);
        channel.register(m -> serialCount.incrementAndGet(), RegisterType.ASYNC_SERIAL);

        // init input test messages
        final long nb = 1000l;
        for (int i = 1; i <= nb; i++) {
            channel.post(String.valueOf(i));
        }

        await().atMost(10, SECONDS).until(() -> nb == serialCount.get());

        assertEquals(nb, syncCount.get());
        assertEquals(nb, asyncCount.get());
        assertEquals(nb, serialCount.get());
    }

    @Test
    public void perfChannelMultipleSubscriberTest() throws Exception {
        Channel<String> channel = eventbus.createChannel("perf", String.class);

        final AtomicLong syncCount = new AtomicLong();
        final AtomicLong asyncCount = new AtomicLong();
        final AtomicLong serialCount1 = new AtomicLong();
        final AtomicLong serialCount2 = new AtomicLong();
        final AtomicLong serialCount3 = new AtomicLong();

        channel.register(m -> syncCount.incrementAndGet(), RegisterType.SYNC);
        channel.register(m -> asyncCount.incrementAndGet(), RegisterType.ASYNC);
        channel.register(m -> serialCount1.incrementAndGet(), RegisterType.ASYNC_SERIAL);
        channel.register(m -> serialCount2.incrementAndGet(), RegisterType.ASYNC_SERIAL);
        channel.register(m -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, RegisterType.ASYNC_SERIAL);

        // init input test messages
        final long nb = 1000l;
        for (int i = 1; i <= nb; i++) {
            channel.post(String.valueOf(i));
        }

        await().atMost(10, SECONDS).until(() -> nb == serialCount1.get());

        assertEquals(nb, syncCount.get());
        assertEquals(nb, asyncCount.get());
        assertEquals(nb, serialCount1.get());
    }

    private void test(RegisterType registerType, List<String> inputMessages, List<String> processedMessages) {
        test(registerType, inputMessages, s -> processedMessages.add(s));
        await().atMost(2, SECONDS).until(() -> inputMessages.size() == processedMessages.size());
    }

    private void test(RegisterType registerType, List<String> inputMessages, Consumer<String> consumer) {
        Channel<String> channel = eventbus.createChannel("Test", String.class);

        channel.register(consumer, registerType);

        inputMessages.forEach(msg -> channel.post(msg));
    }

    private static List<String> createMessages(int nb) {
        List<String> inputMessages = new ArrayList<>();

        // init input test messages
        for (int i = 1; i <= nb; i++) {
            inputMessages.add("msg"+i);
        }
        return inputMessages;
    }

}