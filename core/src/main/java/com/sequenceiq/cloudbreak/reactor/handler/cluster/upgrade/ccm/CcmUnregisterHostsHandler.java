package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.CcmUpgradeService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.CcmUnregisterHostsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.CcmUnregisterHostsResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.CcmUpgradeFailedEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class CcmUnregisterHostsHandler extends ExceptionCatcherEventHandler<CcmUnregisterHostsRequest> {

    @Inject
    private CcmUpgradeService ccmUpgradeService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CcmUnregisterHostsRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<CcmUnregisterHostsRequest> event) {
        return new CcmUpgradeFailedEvent(resourceId, e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<CcmUnregisterHostsRequest> event) {
        CcmUnregisterHostsRequest request = event.getData();
        Long stackId = request.getResourceId();
        ccmUpgradeService.unregister(stackId);
        return new CcmUnregisterHostsResult(stackId);
    }
}
