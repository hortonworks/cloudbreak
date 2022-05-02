package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmReconfigureNginxRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmReconfigureNginxResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class ReconfigureNginxHandler extends ExceptionCatcherEventHandler<UpgradeCcmReconfigureNginxRequest> {

    @Inject
    private UpgradeCcmService upgradeCcmService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeCcmReconfigureNginxRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeCcmReconfigureNginxRequest> event) {
        return new UpgradeCcmFailedEvent(resourceId, e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<UpgradeCcmReconfigureNginxRequest> event) {
        UpgradeCcmReconfigureNginxRequest request = event.getData();
        Long stackId = request.getResourceId();
        upgradeCcmService.reconfigureNginx(stackId);
        return new UpgradeCcmReconfigureNginxResult(stackId);
    }
}
