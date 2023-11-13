package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.START_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_START_FAILED;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.conclusion.ConclusionCheckerService;
import com.sequenceiq.cloudbreak.conclusion.ConclusionCheckerType;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterCreationFailedRequest;
import com.sequenceiq.cloudbreak.service.stackstatus.StackStatusService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ClusterCreationFailedHandler extends ExceptionCatcherEventHandler<ClusterCreationFailedRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCreationFailedHandler.class);

    @Inject
    private ConclusionCheckerService conclusionCheckerService;

    @Inject
    private StackStatusService stackStatusService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterCreationFailedRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterCreationFailedRequest> event) {
        return new StackEvent(ClusterCreationEvent.CLUSTER_CREATION_FAILURE_HANDLED_EVENT.event(), resourceId);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterCreationFailedRequest> event) {
        ClusterCreationFailedRequest request = event.getData();
        LOGGER.info("Handle ClusterCreationFailedRequest, stackId: {}", request.getResourceId());
        ConclusionCheckerType conclusionCheckerType = getConclusionCheckerType(request.getResourceId());
        conclusionCheckerService.runConclusionChecker(request.getResourceId(), START_FAILED.name(), CLUSTER_START_FAILED, conclusionCheckerType);
        return new StackEvent(ClusterCreationEvent.CLUSTER_CREATION_FAILURE_HANDLED_EVENT.event(), request.getResourceId());
    }

    private ConclusionCheckerType getConclusionCheckerType(Long stackId) {
        Set<DetailedStackStatus> detailedStackStatuses = stackStatusService.findAllStackStatusesById(stackId).stream()
                .map(StackStatus::getDetailedStackStatus).collect(Collectors.toSet());

        if (detailedStackStatuses.contains(DetailedStackStatus.STARTING_CLUSTER_SERVICES)) {
            return ConclusionCheckerType.DEFAULT;
        } else if (detailedStackStatuses.contains(DetailedStackStatus.VALIDATING_CLOUD_STORAGE_ON_VM)) {
            return ConclusionCheckerType.CLUSTER_PROVISION_AFTER_SALT_BOOTSTRAP;
        } else {
            return ConclusionCheckerType.CLUSTER_PROVISION_BEFORE_SALT_BOOTSTRAP;
        }
    }
}
