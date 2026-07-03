package com.sequenceiq.freeipa.flow.freeipa.downscale.handler;

import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.VERIFY_REPLICATION_CLEANUP_FAILED_EVENT;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.replicationcleanup.VerifyReplicationCleanupRequest;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.replicationcleanup.VerifyReplicationCleanupResponse;
import com.sequenceiq.freeipa.service.freeipa.cleanup.VerifyReplicationCleanupService;

@Component
public class VerifyReplicationCleanupHandler extends ExceptionCatcherEventHandler<VerifyReplicationCleanupRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VerifyReplicationCleanupHandler.class);

    @Inject
    private VerifyReplicationCleanupService verifyReplicationCleanupService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(VerifyReplicationCleanupRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<VerifyReplicationCleanupRequest> event) {
        return new DownscaleFailureEvent(VERIFY_REPLICATION_CLEANUP_FAILED_EVENT.event(),
                resourceId, "Downscale Verify Replication Cleanup", Set.of(), Map.of(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<VerifyReplicationCleanupRequest> event) {
        VerifyReplicationCleanupRequest request = event.getData();
        try {
            verifyReplicationCleanupService.verifyOnSurvivingMasters(request.getResourceId(), request.getHosts());
            return new VerifyReplicationCleanupResponse(request.getResourceId());
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.error("Downscale verify replication cleanup failed", e);
            return new DownscaleFailureEvent(VERIFY_REPLICATION_CLEANUP_FAILED_EVENT.event(),
                    request.getResourceId(), "Downscale Verify Replication Cleanup", Set.of(), Map.of(), e);
        }
    }
}
