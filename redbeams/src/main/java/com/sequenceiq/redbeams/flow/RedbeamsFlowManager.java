package com.sequenceiq.redbeams.flow;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class RedbeamsFlowManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsFlowManager.class);

    private static final long WAIT_FOR_ACCEPT = 10L;

    private static final long TIMEOUT_MILLIS = 5000;

    private static final long SLEEP_MILLIS = 100;

    @Inject
    private EventBus reactor;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private Clock clock;

    private final Random random = ThreadLocalRandom.current();

    public void triggerHelloworld() {
        String selector = "HELLOWORLD_CHAIN_EVENT";
        notify(selector, new BaseFlowEvent(selector, random.nextLong(), "resourceCrn"));
    }

    public void notify(String selector, Acceptable acceptable) {
        Map<String, Object> headerWithUserCrn = getHeaderWithUserCrn(null);
        Event<Acceptable> event = eventFactory.createEventWithErrHandler(headerWithUserCrn, acceptable);
        notify(selector, event);
    }

    public void notify(String selector, Acceptable acceptable, Map<String, Object> headers) {
        Map<String, Object> headerWithUserCrn = getHeaderWithUserCrn(headers);
        Event<Acceptable> event = eventFactory.createEventWithErrHandler(headerWithUserCrn, acceptable);
        notify(selector, event);
    }

    public void notify(Selectable selectable) {
        Event<Selectable> event = eventFactory.createEvent(selectable);
        reactor.notify(selectable.selector(), event);
    }

    private void notify(String selector, Event<Acceptable> event) {
        reactor.notify(selector, event);
        try {
            Boolean accepted = true;
            if (event.getData().accepted() != null) {
                accepted = event.getData().accepted().await(WAIT_FOR_ACCEPT, TimeUnit.SECONDS);
            }
            if (accepted == null || !accepted) {
                throw new RuntimeException(String.format("Flows under operation, request not allowed."));
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public void cancelRunningFlows(Long dbstackId) {
        RedbeamsEvent cancelEvent = new RedbeamsEvent(Flow2Handler.FLOW_CANCEL, dbstackId);
        reactor.notify(Flow2Handler.FLOW_CANCEL, eventFactory.createEventWithErrHandler(cancelEvent));
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
