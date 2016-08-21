package com.wayoos.eventbus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

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
    public void createChannel() throws Exception {
        assertNotNull(eventbus.createChannel("test", String.class));
    }

    @Test
    public void getNotCreatedChannel() throws Exception {
        assertNull(eventbus.getChannel("test", String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getChannelInvalidMessageType() throws Exception {
        eventbus.createChannel("test", String.class);
        eventbus.getChannel("test", Integer.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void postChannelInvalidMessageType() throws Exception {
        Channel channel = eventbus.createChannel("test", String.class);

        channel.post(23l);
    }

}