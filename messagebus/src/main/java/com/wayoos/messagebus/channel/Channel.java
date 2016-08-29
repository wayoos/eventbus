package com.wayoos.messagebus.channel;

import com.wayoos.messagebus.MessagebusExecutorFactory;
import com.wayoos.messagebus.RegisterType;
import com.wayoos.messagebus.event.EventType;
import com.wayoos.messagebus.event.MessagebusEvent;
import com.wayoos.messagebus.event.MessagebusEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executor;
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

    private final MessagebusExecutorFactory messagebusExecutorFactory;

    private final Class<T> messageType;

    private final MessagebusEventListener messagebusEventListener;

    public Channel(MessagebusExecutorFactory messagebusExecutorFactory, Class<T> messageType,
                   MessagebusEventListener messagebusEventListener) {
        this.messagebusExecutorFactory = messagebusExecutorFactory;
        this.messageType = messageType;
        this.messagebusEventListener = messagebusEventListener;

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



        syncConsumers.forEach(c -> processMessage(RegisterType.SYNC, c, message));

        aSyncConsumers.forEach(c -> executorService.execute(() -> processMessage(RegisterType.ASYNC, c, message)));

        aSyncSequentialConsumers.forEach(c -> c.getSerialExecutor().execute(() -> processMessage(RegisterType.ASYNC_SERIAL, c.getConsumer(), message)));
    }

    private void sendEvent(Object message, RegisterType registerType, EventType eventType) {
        messagebusEventListener.onEvent(new MessagebusEvent() {
            @Override
            public String getChannelId() {
                return "id";
            }

            @Override
            public Object getMessage() {
                return message;
            }

            @Override
            public RegisterType getRegisterType() {
                return null;
            }

            @Override
            public EventType getEventType() {
                return eventType;
            }
        });
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
