package com.sequenceiq.flow.core;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;

/**
 * Support class to make unit testing of {@link AbstractAction} easier.
 * @param <S> flow state type
 * @param <E> flow event type
 * @param <C> flow context type
 * @param <P> request event payload type
 */
public class AbstractActionTestSupport<S extends FlowState, E extends FlowEvent, C extends CommonContext, P extends Payload> {

    private final AbstractAction<S, E, C, P> action;

    /**
     * Creates a new {@code AbstractActionTestSupport} instance wrapping {@code action}.
     * @param action {@link AbstractAction} instance to wrap; must not be {@code null}
     * @throws NullPointerException if {@code action == null}
     */
    public AbstractActionTestSupport(AbstractAction<S, E, C, P> action) {
        this.action = Objects.requireNonNull(action);
    }

    /**
     * Delegates to {@link AbstractAction#prepareExecution(Payload, Map)}.
     * @param payload request event payload
     * @param variables extended state variables
     */
    public void prepareExecution(P payload, Map<Object, Object> variables) {
        action.prepareExecution(payload, variables);
    }

    /**
     * Delegates to {@link AbstractAction#createRequest(CommonContext)}.
     * @param context flow context
     * @return request event payload
     */
    public Selectable createRequest(C context) {
        return action.createRequest(context);
    }

    /**
     * Delegates to {@link AbstractAction#doExecute(CommonContext, Payload, Map)}.
     * @param context flow context
     * @param payload request event payload
     * @param variables extended state variables
     * @throws Exception if some error is encountered during execution
     */
    public void doExecute(C context, P payload, Map<Object, Object> variables) throws Exception {
        action.doExecute(context, payload, variables);
    }

    /**
     * Delegates to {@link AbstractAction#createFlowContext(flowParameters, stateContext, Payload)}.
     * @param flowParameters flow paramters
     * @param stateContext State Context
     * @param payload request event payload
     */
    public C createFlowContext(FlowParameters flowParameters, StateContext<S, E> stateContext, P payload) {
        return action.createFlowContext(flowParameters, stateContext, payload);
    }

    /**
     * Delegates to {@link AbstractAction#getFailurePayload(Payload, Optional, Exception)}.
     * @param payload request event payload
     * @param flowContext optional containing the flow context
     * @param ex failure exception
     * @return payload for the failure
     */
    public Object getFailurePayload(P payload, Optional<C> flowContext, Exception ex) {
        return action.getFailurePayload(payload, flowContext, ex);
    }

}
