package com.sequenceiq.environment.environment.flow.encryptionprofile;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.logger.MdcContext;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

abstract class AbstractEncryptionProfileAction<S extends FlowState, E extends FlowEvent, P extends BaseNamedFlowEvent>
        extends AbstractAction<S, E, CommonContext, P> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEncryptionProfileAction.class);

    private static final String UNKNOWN_ERROR = "Unknown error";

    private final BiFunction<BaseNamedFlowEvent, Exception, ? extends BaseFailedFlowEvent> failedEventFactory;

    protected AbstractEncryptionProfileAction(Class<P> payloadClass,
            BiFunction<BaseNamedFlowEvent, Exception, ? extends BaseFailedFlowEvent> failedEventFactory) {
        super(payloadClass);
        this.failedEventFactory = failedEventFactory;
    }

    @Override
    protected CommonContext createFlowContext(FlowParameters flowParameters, StateContext<S, E> stateContext, P payload) {
        return new CommonContext(flowParameters);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<CommonContext> flowContext, Exception ex) {
        return failedEventFactory.apply(payload, ex);
    }

    protected String getFailureReason(Exception exception) {
        return Optional.ofNullable(exception)
                .map(Exception::getMessage)
                .filter(StringUtils::isNotBlank)
                .orElse(UNKNOWN_ERROR);
    }

    @Override
    protected void prepareExecution(P payload, Map<Object, Object> variables) {
        if (payload != null) {
            MdcContext.builder().resourceCrn(payload.getResourceCrn()).buildMdc();
        } else {
            LOGGER.warn("Payload was null in prepareExecution so resourceCrn cannot be added to the MdcContext!");
        }
    }
}
