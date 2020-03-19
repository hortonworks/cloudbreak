package com.sequenceiq.redbeams.flow;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.model.FlowAcceptResult;
import com.sequenceiq.flow.core.model.ResultType;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class RedbeamsFlowManager {

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

    private void notify(String selector, Event<Acceptable> event) {
        reactor.notify(selector, event);
        try {
            FlowAcceptResult accepted = (FlowAcceptResult) event.getData().accepted().await(WAIT_FOR_ACCEPT, TimeUnit.SECONDS);
            if (accepted == null || ResultType.ALREADY_EXISTING_FLOW.equals(accepted.getResultType())) {
                throw new RuntimeException(String.format("Flows under operation, request not allowed."));
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
