package com.sequenceiq.freeipa.service.freeipa.flow;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.model.FlowAcceptResult;
import com.sequenceiq.flow.core.model.ResultType;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;

import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.rx.Promise;

@Component
public class FreeIpaFlowManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaFlowManager.class);

    private static final long WAIT_FOR_ACCEPT = 10L;

    @Inject
    private EventBus reactor;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    public void notify(String selector, Acceptable acceptable) {
        Map<String, Object> headerWithUserCrn = getHeaderWithUserCrn(null);
        Event<Acceptable> event = eventFactory.createEventWithErrHandler(headerWithUserCrn, acceptable);
        notify(selector, event);
    }

    public void notify(Selectable selectable) {
        Event<Selectable> event = eventFactory.createEvent(selectable);
        LOGGER.debug("Notify reactor for selector [{}] with event [{}]", selectable.selector(), event);
        reactor.notify(selectable.selector(), event);
    }

    public void notify(BaseFlowEvent selectable, Event.Headers headers) {
        Event<BaseFlowEvent> event = eventFactory.createEventWithErrHandler(new HashMap<>(headers.asMap()), selectable);
        LOGGER.debug("Notify reactor for selector [{}] with event [{}]", selectable.selector(), event);
        reactor.notify(selectable.selector(), event);
        checkFlowOperationForResource(event);
    }

    private void notify(String selector, Event<Acceptable> event) {
        LOGGER.debug("Notify reactor for selector [{}] with event [{}]", selector, event);
        reactor.notify(selector, event);
        checkFlowOperationForResource(event);
    }

    private void checkFlowOperationForResource(Event<? extends Acceptable> event) {
        try {
            Promise<AcceptResult> acceptPromise = event.getData().accepted();
            FlowAcceptResult accepted = (FlowAcceptResult) acceptPromise.await(WAIT_FOR_ACCEPT, TimeUnit.SECONDS);
            if (accepted == null || ResultType.ALREADY_EXISTING_FLOW.equals(accepted.getResultType())) {
                throw new RuntimeException("Flows under operation, request not allowed.");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private Map<String, Object> getHeaderWithUserCrn(Map<String, Object> headers) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        Map<String, Object> decoratedHeader;
        decoratedHeader = headers != null ? new HashMap<>(headers) : new HashMap<>();
        if (StringUtils.isNotBlank(userCrn)) {
            decoratedHeader.put(FlowConstants.FLOW_TRIGGER_USERCRN, userCrn);
        }
        return decoratedHeader;
    }
}
