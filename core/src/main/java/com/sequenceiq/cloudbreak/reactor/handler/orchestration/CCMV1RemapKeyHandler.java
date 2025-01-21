package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CCMV1KeyRemapper;
import com.sequenceiq.cloudbreak.ccm.key.CcmResourceUtil;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyEnablementService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.CCMV1RemapKeyRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyReRegistrationResult;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class CCMV1RemapKeyHandler extends ExceptionCatcherEventHandler<CCMV1RemapKeyRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CCMV1RemapKeyHandler.class);

    @Inject
    private ClusterProxyEnablementService clusterProxyEnablementService;

    @Inject
    private StackService stackService;

    @Inject
    @Qualifier("DefaultCCMV1KeyRemapper")
    private CCMV1KeyRemapper ccmV1KeyRemapper;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CCMV1RemapKeyRequest.class);
    }

    @Override
    public Selectable defaultFailureEvent(Long resourceId, Exception e, Event<CCMV1RemapKeyRequest> event) {
        return new ClusterProxyReRegistrationResult(e.getMessage(), e, event.getData());
    }

    @Override
    public Selectable doAccept(HandlerEvent<CCMV1RemapKeyRequest> event) {
        CCMV1RemapKeyRequest request = event.getData();

        if (!clusterProxyEnablementService.isClusterProxyApplicable(request.getCloudPlatform())) {
            LOGGER.info("Cluster proxy integration is disabled. Skipping CCMV1 remap.");
            return new StackEvent(request.getFinishedEvent(), request.getResourceId());
        }

        Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
        String actorCrn = ThreadBasedUserCrnProvider.getUserCrn();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String originalKeyId = CcmResourceUtil.getKeyId(request.getOriginalCrn());
        String newKeyId = CcmResourceUtil.getKeyId(stack.getResourceCrn());
        try {
            assert actorCrn != null;
            ccmV1KeyRemapper.remapKey(actorCrn, accountId, originalKeyId, newKeyId);
            return new StackEvent(request.getFinishedEvent(), request.getResourceId());
        } catch (Exception ex) {
            LOGGER.error("Failed to remap CCMv1 key.", ex);
            return new ClusterProxyReRegistrationResult(ex.getMessage(), ex, request);
        }
    }
}
