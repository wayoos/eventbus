package com.wayoos.eventbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Created by steph on 21.08.16.
 */
public class Channel<T> {

    private final static Logger logger = LoggerFactory.getLogger(Channel.class);

    Set<Consumer> syncConsumers = Collections.synchronizedSet(new HashSet<>());
    Set<Consumer> aSyncConsumers = Collections.synchronizedSet(new HashSet<>());
    Set<SerialContext> aSyncSequentialConsumers = Collections.synchronizedSet(new HashSet<>());

    final Executor executorService;

    private final EventbusExecutorFactory eventbusExecutorFactory;

    private final Class<T> messageType;

    public Channel(EventbusExecutorFactory eventbusExecutorFactory, Class<T> messageType) {
        this.eventbusExecutorFactory = eventbusExecutorFactory;
        this.messageType = messageType;
        executorService = eventbusExecutorFactory.getExecutor();
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
        if (!messageType().isAssignableFrom(message.getClass()))
            throw new IllegalArgumentException("Invalid message type");

        syncConsumers.forEach(c -> processMessage(RegisterType.SYNC, c, message));

        aSyncConsumers.forEach(c -> executorService.execute(() -> processMessage(RegisterType.ASYNC, c, message)));

        aSyncSequentialConsumers.forEach(c -> c.getSerialExecutor().execute(() -> processMessage(RegisterType.ASYNC_SERIAL, c.getConsumer(), message)));
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
//        if (!aSyncSequentialConsumers.contains(consumer)) {
        SerialContext<T> serialContext = new SerialContext<>(consumer, new SerialExecutor(executorService));
        aSyncSequentialConsumers.add(serialContext);
//        }
    }

    class SerialContext<T> {

        private final Consumer<T> consumer;
        private final SerialExecutor serialExecutor;

        public SerialContext(Consumer<T> consumer, SerialExecutor serialExecutor) {
            this.consumer = consumer;
            this.serialExecutor = serialExecutor;
        }

        public Consumer<T> getConsumer() {
            return consumer;
        }

        public SerialExecutor getSerialExecutor() {
            return serialExecutor;
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
