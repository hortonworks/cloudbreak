package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.CcmUpgradeService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.CcmRemoveAutoSshRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.CcmRemoveAutoSshResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.CcmUpgradeFailedEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class CcmRemoveAutoSshHandler extends ExceptionCatcherEventHandler<CcmRemoveAutoSshRequest> {

    @Inject
    private CcmUpgradeService ccmUpgradeService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CcmRemoveAutoSshRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<CcmRemoveAutoSshRequest> event) {
        return new CcmUpgradeFailedEvent(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<CcmRemoveAutoSshRequest> event) {
        CcmRemoveAutoSshRequest request = event.getData();
        Long stackId = request.getResourceId();
        ccmUpgradeService.removeAutoSsh(stackId);
        return new CcmRemoveAutoSshResult(stackId);
    }
}
