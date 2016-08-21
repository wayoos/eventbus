package com.wayoos.eventbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Created by steph on 21.08.16.
 */
public class Channel<T> {

    private final static Logger logger = LoggerFactory.getLogger(Channel.class);

    Set<Consumer> syncConsumers = new HashSet<>();
    Set<Consumer> aSyncConsumers = new HashSet<>();
    Set<Consumer> aSyncSequentialConsumers = new HashSet<>();

    ExecutorService executorService = Executors.newCachedThreadPool();

    SerialExecutor serialExecutor = new SerialExecutor(executorService);

    private final Class<T> messageType;

    public Channel(Class<T> messageType) {
        this.messageType = messageType;
    }

    public Class<T> messageType() {
        return messageType;
    }

    public void register(Consumer<T> consumer, RegisterType registerType) {
        switch (registerType) {
            case SYNC:
                addSync(consumer);
                break;
            case ASYNC:
                addAsync(consumer);
                break;
            case ASYNC_SERIAL:
                addAsyncSequential(consumer);
                break;
        }
    }

    public void unregister(Consumer<T> consumer) {

    }

    /**
     * Post a message in the channel
     * @param message
     */
    public void post(final T message) {
        // check message type
        if (!messageType().equals(message.getClass()))
            throw new IllegalArgumentException("Invalid message type");

        syncConsumers.forEach(c -> processMessage(RegisterType.SYNC, c, message));

        aSyncConsumers.forEach(c -> executorService.execute(() -> processMessage(RegisterType.ASYNC, c, message)));

        aSyncSequentialConsumers.forEach(c -> serialExecutor.execute(() -> processMessage(RegisterType.ASYNC_SERIAL, c, message)));
    }

    private void processMessage(RegisterType registerType, Consumer c, T message) {
        logger.debug("Before {} message {} is accept by consumer {}", registerType, message, c);
        try {
            c.accept(message);
        } catch (Throwable e) {
            logger.error("Consumer accept message error.", e);
        }
    }

    private void addSync(Consumer<T> consumer) {
        if (!syncConsumers.contains(consumer)) {
            syncConsumers.add(consumer);
        }
    }

    private void addAsync(Consumer<T> consumer) {
        if (!aSyncConsumers.contains(consumer)) {
            aSyncConsumers.add(consumer);
        }
    }

    private void addAsyncSequential(Consumer<T> consumer) {
        if (!aSyncSequentialConsumers.contains(consumer)) {
            aSyncSequentialConsumers.add(consumer);
        }
    }


    class SerialExecutor implements Executor {
        final Queue<Runnable> tasks = new ArrayDeque<>();
        final Executor executor;
        Runnable active;

        SerialExecutor(Executor executor) {
            this.executor = executor;
        }

        public synchronized void execute(final Runnable r) {
            tasks.offer(new Runnable() {
                public void run() {
                    try {
                        r.run();
                    } finally {
                        scheduleNext();
                    }
                }
            });
            if (active == null) {
                scheduleNext();
            }
        }

        protected synchronized void scheduleNext() {
            if ((active = tasks.poll()) != null) {
                executor.execute(active);
            }
        }
    }

}
