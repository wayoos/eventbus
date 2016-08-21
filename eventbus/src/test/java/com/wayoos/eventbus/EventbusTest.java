package com.wayoos.eventbus;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by steph on 21.08.16.
 */
public class EventbusTest {


    @Test
    public void createChannel() throws Exception {
        Eventbus eventbus = new Eventbus();

        assertNotNull(eventbus.createChannel("test", String.class));
    }

    @Test
    public void getNotCreatedChannel() throws Exception {
        Eventbus eventbus = new Eventbus();

        assertNull(eventbus.getChannel("test", String.class));
    }


}