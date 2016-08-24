package com.wayoos.messagebus;

import com.wayoos.messagebus.channel.Channel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by steph on 21.08.16.
 */
public class MessagebusTest {

    Messagebus messagebus;

    @Before
    public void beforeTest() {
        messagebus = new Messagebus();
    }

    @After
    public void afterTest() {
        // TODO call shutdown
        messagebus = null;
    }

    @Test
    public void createChannel() throws Exception {
        Channel<String> channel = messagebus.createChannel("test", String.class);
        assertNotNull(channel);

        Channel<String> sameChannel = messagebus.createChannel("test", String.class);
        assertTrue(channel == sameChannel);

        Channel<String> getChannel = messagebus.createChannel("test", String.class);
        assertTrue(channel == getChannel);
    }

    @Test
    public void getNotCreatedChannel() throws Exception {
        assertNull(messagebus.getChannel("test", String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getChannelInvalidMessageType() throws Exception {
        messagebus.createChannel("test", String.class);
        messagebus.getChannel("test", Integer.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void postChannelInvalidMessageType() throws Exception {
        Channel channel = messagebus.createChannel("test", String.class);

        channel.post(23l);
    }

    @Test
    public void postChannelSubMessageType() throws Exception {
        Channel channel = messagebus.createChannel("test", Number.class);

        channel.post(23l);
    }


}