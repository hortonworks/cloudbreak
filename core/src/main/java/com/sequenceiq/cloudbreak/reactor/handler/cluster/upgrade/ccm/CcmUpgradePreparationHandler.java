package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.CcmUpgradeService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.CcmUpgradePreparationFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.CcmUpgradePreparationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.CcmUpgradePreparationResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class CcmUpgradePreparationHandler extends ExceptionCatcherEventHandler<CcmUpgradePreparationRequest> {

    @Inject
    private CcmUpgradeService ccmUpgradeService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CcmUpgradePreparationRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<CcmUpgradePreparationRequest> event) {
        return new CcmUpgradePreparationFailed(resourceId, e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<CcmUpgradePreparationRequest> event) {
        Selectable result;
        CcmUpgradePreparationRequest request = event.getData();
        Long stackId = request.getResourceId();
        ccmUpgradeService.prepare(stackId);
        return new CcmUpgradePreparationResult(stackId);
    }
}
