package com.sequenceiq.cloudbreak.core.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.event.Event;
import reactor.function.Consumer;

/**
 * Abstract base class for reactor event consumers.
 * <p/>
 * A flow handler instance receives a reactor event and has the following responsibilities:
 * 1. performs the application logic on the received data (usually by calling a service)
 * 2. translates service exceptions if any
 * 3. delegates to the given error consumer if an error occurs
 * 4. eventually proceeds forward by firing a new reactor notification
 *
 * @param <T> the type of the event payload
 */
public abstract class AbstractFlowHandler<T> implements Consumer<Event<T>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFlowHandler.class);

    /**
     * Template method for flow reactor event consumer logic.
     *
     * @param event the reactor event to be acted on
     */
    @Override
    public void accept(Event<T> event) {
        try {
            LOGGER.debug("FlowHandler called with event {}", event);
            execute(event);
            triggerNext();
        } catch (Throwable t) {
            LOGGER.debug("Consuming error {}", t);
            event.consumeError(t);
        }
    }

    /**
     * This method is the place for the handler's logic. After extracting the event payload it typically delegates to a specialized service
     *
     * @param event the reactor event received
     * @throws Throwable if an error occurs during the handler logic execution.
     */
    protected abstract void execute(Event<T> event) throws Exception;

    /**
     * Send a reactor notification to trigger the next step of the flow if required.
     */
    protected abstract void triggerNext();

}
