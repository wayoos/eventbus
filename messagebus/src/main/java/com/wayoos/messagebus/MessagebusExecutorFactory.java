package com.wayoos.messagebus;

import java.util.concurrent.Executor;

/**
 * Created by steph on 22.08.16.
 */
public interface MessagebusExecutorFactory {

    /**
     * This method is call for each created channel. you can return the same executor
     * to share a thread pool to all channel. Or you can create a new Executor for each Channel.
     * @return
     */
    Executor getExecutor();

}
