package com.wayoos.eventbus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

/**
 * Created by steph on 21.08.16.
 */
public class EventbusTest {

    Eventbus eventbus;

    @Before
    public void beforeTest() {
        eventbus = new Eventbus();
    }

    @After
    public void afterTest() {
        // TODO call shutdown
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

    private void test(RegisterType registerType, List<String> inputMessages, List<String> processedMessages) {
        Channel<String> channel = eventbus.getChannel("Test", String.class);

        channel.register(s -> processedMessages.add(s), registerType);

        inputMessages.forEach(msg -> channel.post(msg));

        await().atMost(2, SECONDS).until(() -> inputMessages.size() == processedMessages.size());
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