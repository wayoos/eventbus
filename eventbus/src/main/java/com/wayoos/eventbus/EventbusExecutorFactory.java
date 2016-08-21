package com.wayoos.eventbus;

import java.util.concurrent.Executor;

/**
 * Created by steph on 22.08.16.
 */
public interface EventbusExecutorFactory {

    Executor getExecutor();

}
