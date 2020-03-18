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
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.model.FlowAcceptResult;
import com.sequenceiq.flow.core.model.ResultType;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.Event;
import reactor.bus.EventBus;

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
        LOGGER.debug("Notify reactor with event [{}]", event);
        reactor.notify(selectable.selector(), event);
    }

    private void notify(String selector, Event<Acceptable> event) {
        LOGGER.debug("Notify reactor for selector [{}] with event [{}]", selector, event);
        reactor.notify(selector, event);
        try {
            FlowAcceptResult accepted = (FlowAcceptResult) event.getData().accepted().await(WAIT_FOR_ACCEPT, TimeUnit.SECONDS);
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
