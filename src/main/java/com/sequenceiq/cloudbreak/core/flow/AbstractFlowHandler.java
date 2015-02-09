package com.sequenceiq.cloudbreak.core.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import reactor.core.Reactor;
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

    @Autowired
    private FlowManager flowManager;

    @Autowired
    private FlowEventFactory eventFactory;

    @Autowired
    private Reactor reactor;

    /**
     * Template method for flow reactor event consumer logic.
     *
     * @param event the reactor event to be acted on
     */
    @Override
    public void accept(Event<T> event) {
        Object result = null;
        boolean success = false;
        try {
            LOGGER.debug("FlowHandler called with event {}", event);
            result = execute(event);
            success = true;
        } catch (Throwable t) {
            LOGGER.debug("Consuming the error {}", t);
            event.consumeError(t);
            handleErrorFlow(t, event);
        }
        Object payload = assemblePayload(result);
        next(payload, success);
    }

    /**
     * This method is the place for the handler's logic. After extracting the event payload it typically delegates to a specialized service
     *
     * @param event the reactor event received
     * @throws Throwable if an error occurs during the handler logic execution.
     */
    protected abstract Object execute(Event<T> event) throws Exception;

    /**
     * Send a reactor notification to trigger the next step of the flow if required.
     */
    protected void next(Object payload, boolean success) {
        String key = flowManager.transition(this.getClass(), success);
        if (null != key) {
            Event event = eventFactory.createEvent(payload, key);
            reactor.notify(key, event);
        } else {
            LOGGER.debug("The handler {} has no transitions.", this.getClass());
        }
    }

    /**
     * Delegates to the error handling callback. Enhances the error with contextual data from the passed in event.
     *
     * @param throwable the caught exception
     * @param data      the received
     */

    protected abstract void handleErrorFlow(Throwable throwable, Object data);

    /**
     * Assembles the payload for the next phase based on the results of the execution.
     *
     * @param serviceResult the results of the execution
     * @return the payload for the next phase of the flow
     */
    protected abstract Object assemblePayload(Object serviceResult);

}
