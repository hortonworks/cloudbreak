package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmHealthCheckRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmHealthCheckResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class HealthCheckHandler extends ExceptionCatcherEventHandler<UpgradeCcmHealthCheckRequest> {

    @Inject
    private UpgradeCcmService upgradeCcmService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeCcmHealthCheckRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeCcmHealthCheckRequest> event) {
        return new UpgradeCcmFailedEvent(resourceId, e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<UpgradeCcmHealthCheckRequest> event) {
        UpgradeCcmHealthCheckRequest request = event.getData();
        Long stackId = request.getResourceId();
        upgradeCcmService.healthCheck(stackId);
        return new UpgradeCcmHealthCheckResult(stackId);
    }
}
