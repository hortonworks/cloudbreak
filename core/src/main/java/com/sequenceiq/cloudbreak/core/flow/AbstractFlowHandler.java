package com.sequenceiq.cloudbreak.core.flow;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.DefaultFlowContext;
import com.sequenceiq.cloudbreak.core.flow.service.FlowFacade;

import reactor.bus.Event;
import reactor.fn.Consumer;


/**
 * Abstract base class for reactor event consumers.
 * <p/>
 * A flow handler instance receives a reactor event and has the following responsibilities:
 * <p/>
 * 1. performs the application logic on the received data (usually by delegating to a service)
 * 2. delegates to the given error consumer if an error occurs
 * 3. triggers the appropriate error flow if required
 * 4. eventually proceeds forward to the next phase
 *
 * @param <T> the type of the event payload
 */
public abstract class AbstractFlowHandler<T extends DefaultFlowContext> implements Consumer<Event<T>>, FlowHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFlowHandler.class);

    @Inject
    private FlowManager flowManager;

    @Inject
    private FlowFacade flowFacade;

    /**
     * Template method for flow reactor event consumer logic.
     *
     * @param event the reactor event to be acted on
     */
    @Override
    public void accept(Event<T> event) {
        LOGGER.debug("Executing flow Logic on the event: {}", event);
        Object result = null;
        boolean success = false;
        try {
            result = execute(event);
            success = true;
        } catch (Exception t) {
            if (t instanceof FlowCancelledException || t.getCause() instanceof FlowCancelledException) {
                LOGGER.warn("Flow was cancelled: {}", t.getMessage());
                return;
            }
            consumeError(event, t);
            try {
                result = handleErrorFlow(t, event.getData());
            } catch (Exception e) {
                LOGGER.error("Error during error handling flow");
                throw new CloudbreakFlowException(e);
            }
        }
        Object payload = assemblePayload(result);
        next(payload, success);
    }

    /**
     * This method is the place for the handler's logic. After extracting the event payload it typically delegates to a specialized service
     *
     * @param event the reactor event received
     * @throws Throwable if an error occurs during the execution of the service logic.
     */
    protected abstract Object execute(Event<T> event) throws CloudbreakException;

    /**
     * Proceeds to the next phase in the flow.
     */
    protected void next(Object payload, boolean success) {
        flowManager.triggerNext(getClass(), payload, success);
    }

    /**
     * Entry point for custom error flows. Implementers are expected to override this method if errors trigger changes in the flow.
     *
     * @param throwable the error occurred
     * @param data      the data received by the handler
     * @return the result of the error processing
     * @throws Exception if the error handling fails
     */
    protected Object handleErrorFlow(Throwable throwable, T data) throws Exception {
        LOGGER.debug("Default error flow handling for {}", getClass());
        data.setErrorReason(throwable.getMessage());
        return data;
    }

    /**
     * Delegates to the error handling callback. Enhances the error with contextual data from the passed in event.
     * Implementers are expected to override this method to perform custom error processing!
     *
     * @param throwable the caught exception
     * @param event     the received data
     */
    protected void consumeError(Event<T> event, Throwable throwable) {
        LOGGER.error(String.format("Error occurred during phase execution: ", throwable.getMessage()), throwable);
        event.consumeError(throwable);
    }

    /**
     * Assembles the payload for the next phase based on the results of the execution.
     * Implementers are expected to override the default behavior if custom decoration is needed!
     *
     * @param serviceResult the results of the execution
     * @return the payload for the next phase of the flow
     */
    protected Object assemblePayload(Object serviceResult) {
        LOGGER.info("Default service result decoration for {}", getClass());
        return serviceResult;
    }

    protected FlowFacade getFlowFacade() {
        return flowFacade;
    }

}
