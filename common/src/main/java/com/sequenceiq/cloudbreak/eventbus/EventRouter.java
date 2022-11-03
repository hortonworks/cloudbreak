package com.sequenceiq.cloudbreak.eventbus;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;

public class EventRouter {

    private final ConcurrentMap<String, Consumer<Event<?>>> handlers;

    private final Consumer<Event<?>> unhandledEventHandler;

    private final BiConsumer<Event<?>, Throwable> exceptionHandler;

    public EventRouter(
            Consumer<Event<?>> unhandledEventHandler,
            BiConsumer<Event<?>, Throwable> exceptionHandler) {
        this.handlers = new ConcurrentHashMap<>();
        this.unhandledEventHandler = Preconditions.checkNotNull(unhandledEventHandler, "unhandledEventHandler must not be null.");
        this.exceptionHandler = Preconditions.checkNotNull(exceptionHandler, "exceptionHandler must not be null.");
    }

    public <T extends Event<?>> void addHandler(String key, Consumer<T> handler) {
        Consumer<Event<?>> previousHandler = handlers.putIfAbsent(key, (Consumer<Event<?>>) handler);
        if (previousHandler != null) {
            throw new IllegalArgumentException(
                    String.format(
                            "Handler is already registered for %s key.%n First registered type: %s,%n second type: %s.",
                            key,
                            previousHandler.getClass().getCanonicalName(),
                            handler.getClass().getCanonicalName()));
        }
    }

    public void handle(Event<?> event) {
        try {
            Consumer<Event<?>> handler = handlers.get(event.getKey());
            if (handler != null) {
                handler.accept(event);
            } else {
                unhandledEventHandler.accept(event);
            }
        } catch (Throwable throwable) {
            exceptionHandler.accept(event, throwable);
        }
    }
}