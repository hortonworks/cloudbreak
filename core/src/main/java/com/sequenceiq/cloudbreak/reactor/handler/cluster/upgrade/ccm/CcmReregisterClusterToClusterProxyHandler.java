package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.CcmUpgradeService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.CcmReregisterToClusterProxyRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.CcmReregisterToClusterProxyResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.CcmUpgradeFailedEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class CcmReregisterClusterToClusterProxyHandler extends ExceptionCatcherEventHandler<CcmReregisterToClusterProxyRequest> {

    @Inject
    private CcmUpgradeService ccmUpgradeService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CcmReregisterToClusterProxyRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<CcmReregisterToClusterProxyRequest> event) {
        return new CcmUpgradeFailedEvent(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<CcmReregisterToClusterProxyRequest> event) {
        CcmReregisterToClusterProxyRequest request = event.getData();
        Long stackId = request.getResourceId();
        ccmUpgradeService.reregister(stackId);
        return new CcmReregisterToClusterProxyResult(stackId);
    }
}
