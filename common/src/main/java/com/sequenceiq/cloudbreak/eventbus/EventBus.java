package com.sequenceiq.cloudbreak.eventbus;

import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;

public class EventBus {

    private final EventRouter eventRouter;

    private final Executor executor;

    public EventBus(EventRouter eventRouter, Executor executor) {
        this.eventRouter = Preconditions.checkNotNull(eventRouter, "eventRouter must not be null.");
        this.executor = Preconditions.checkNotNull(executor, "executor must not be null.");
    }

    public void notify(String key, Event<?> event) {
        Preconditions.checkNotNull(key, "key must not be null.");
        Preconditions.checkNotNull(event, "event must not be null.");
        event.setKey(key);
        executor.execute(() -> eventRouter.handle(event));
    }

    public <T extends Event<?>> void on(String key, Consumer<T> handler) {
        Preconditions.checkNotNull(key, "key must not be null.");
        Preconditions.checkNotNull(handler, "handler must not be null.");
        eventRouter.addHandler(key, handler);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Executor executor;

        private Consumer<Event<?>> unhandledEventHandler;

        private BiConsumer<Event<?>, Throwable> exceptionHandler;

        public Builder executor(Executor executor) {
            this.executor = Preconditions.checkNotNull(executor, "executor must not be null.");
            return this;
        }

        public Builder unhandledEventHandler(Consumer<Event<?>> unhandledEventHandler) {
            this.unhandledEventHandler = Preconditions.checkNotNull(unhandledEventHandler, "unhandledEventHandler must not be null.");
            return this;
        }

        public Builder exceptionHandler(BiConsumer<Event<?>, Throwable> exceptionHandler) {
            this.exceptionHandler = Preconditions.checkNotNull(exceptionHandler, "exceptionHandler must not be null.");
            return this;
        }

        public EventBus build() {
            return new EventBus(new EventRouter(unhandledEventHandler, exceptionHandler), executor);
        }
    }
}
