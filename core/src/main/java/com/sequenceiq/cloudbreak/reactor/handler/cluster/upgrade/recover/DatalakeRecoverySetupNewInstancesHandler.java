package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.recover;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.controller.StackCreatorService;
import com.sequenceiq.cloudbreak.core.flow2.dto.NetworkScaleDetails;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.recovery.bringup.DatalakeRecoverySetupNewInstancesFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.recovery.bringup.DatalakeRecoverySetupNewInstancesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.recovery.bringup.DatalakeRecoverySetupNewInstancesSuccess;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class DatalakeRecoverySetupNewInstancesHandler extends ExceptionCatcherEventHandler<DatalakeRecoverySetupNewInstancesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeRecoverySetupNewInstancesHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private StackCreatorService stackCreatorService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ClusterService clusterService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DatalakeRecoverySetupNewInstancesRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DatalakeRecoverySetupNewInstancesRequest> event) {
        LOGGER.error("DatalakeRecoverySetupNewInstancesHandler step failed with the following message: {}", e.getMessage());
        return new DatalakeRecoverySetupNewInstancesFailedEvent(resourceId, e, DetailedStackStatus.CLUSTER_RECOVERY_FAILED);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<DatalakeRecoverySetupNewInstancesRequest> event) {
        DatalakeRecoverySetupNewInstancesRequest request = event.getData();
        Long stackId = request.getResourceId();
        LOGGER.debug("Setting up new instances for stack {}", stackId);
        try {
            Stack stack = stackService.getByIdWithClusterInTransaction(stackId);
            setupNewInstances(stack);
            clusterService.updateClusterStatusByStackId(stackId, DetailedStackStatus.CLUSTER_RECOVERY_IN_PROGRESS);
            return new DatalakeRecoverySetupNewInstancesSuccess(stackId);
        } catch (Exception e) {
            LOGGER.error("Setting up new instances for stack failed", e);
            return new DatalakeRecoverySetupNewInstancesFailedEvent(stackId, e, DetailedStackStatus.CLUSTER_RECOVERY_FAILED);
        }
    }

    private void setupNewInstances(Stack stack) {
        List<InstanceGroup> instanceGroups = stackCreatorService.sortInstanceGroups(stack);
        for (InstanceGroup instanceGroup : instanceGroups) {
            instanceMetaDataService.saveInstanceAndGetUpdatedStack(stack, Collections.singletonMap(instanceGroup.getGroupName(),
                    instanceGroup.getInitialNodeCount()), Collections.emptyMap(), true, false, NetworkScaleDetails.getEmpty());
        }
    }
}
