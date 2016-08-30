package com.wayoos.messagebus.channel;

import com.wayoos.messagebus.MessagebusExecutorFactory;
import com.wayoos.messagebus.RegisterType;
import com.wayoos.messagebus.event.MessageEventListenerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Created by steph on 21.08.16.
 */
public class Channel<T> implements Closeable {

    private final static Logger logger = LoggerFactory.getLogger(Channel.class);

    Set<Consumer> syncConsumers = Collections.synchronizedSet(new HashSet<>());
    Set<Consumer> aSyncConsumers = Collections.synchronizedSet(new HashSet<>());
    Set<SerialContext> aSyncSequentialConsumers = Collections.synchronizedSet(new HashSet<>());

    private final String alias;

    final Executor executorService;

    private final Class<T> messageType;

    private final MessageEventListenerRegistry messageEventListenerRegistry;

    public Channel(String alias, MessagebusExecutorFactory messagebusExecutorFactory, Class<T> messageType,
                   MessageEventListenerRegistry messageEventListenerRegistry) {
        this.alias = alias;
        this.messageType = messageType;
        this.messageEventListenerRegistry = messageEventListenerRegistry;

        executorService = messagebusExecutorFactory.getExecutor();
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
                addAsyncSerial(consumer);
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

        messageEventListenerRegistry.notifyPostedMessage(alias);

        syncConsumers.forEach(c -> processMessage(RegisterType.SYNC, c, message));

        aSyncConsumers.forEach(c -> executorService.execute(() -> processMessage(RegisterType.ASYNC, c, message)));

        aSyncSequentialConsumers.forEach(c -> c.getSerialExecutor().execute(() -> processMessage(RegisterType.ASYNC_SERIAL, c.getConsumer(), message)));
    }

    @Override
    public void close() throws IOException {

    }

    private void processMessage(RegisterType registerType, Consumer c, T message) {
        logger.debug("Before {} message {} is accept by consumer {}", registerType, message, c);
        try {
            long startTime = System.currentTimeMillis();
            try {
                c.accept(message);
            } finally {
                messageEventListenerRegistry.notifyConsumedMessage(alias, c.getClass().getSimpleName(), System.currentTimeMillis() - startTime);
            }
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

    private void addAsyncSerial(Consumer<T> consumer) {
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
            tasks.offer(() -> {
                try {
                    r.run();
                } finally {
                    scheduleNext();
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
