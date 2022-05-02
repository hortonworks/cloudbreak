package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmPushSaltStatesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmPushSaltStatesResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class PushSaltStateHandler extends ExceptionCatcherEventHandler<UpgradeCcmPushSaltStatesRequest> {

    @Inject
    private UpgradeCcmService upgradeCcmService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeCcmPushSaltStatesRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeCcmPushSaltStatesRequest> event) {
        return new UpgradeCcmFailedEvent(resourceId, e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<UpgradeCcmPushSaltStatesRequest> event) {
        UpgradeCcmPushSaltStatesRequest request = event.getData();
        Long stackId = request.getResourceId();
        upgradeCcmService.pushSaltState(stackId);
        return new UpgradeCcmPushSaltStatesResult(stackId);
    }
}
