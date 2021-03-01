package com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_LB_COLLECT_METADATA;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_LB_CREATE_CLOUD_RESOURCE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_LB_CREATE_ENTITY;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_LB_REGISTER_FREEIPA_DNS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_LB_REGISTER_PUBLIC_DNS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_LB_RESTART_CM;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_LB_UPDATE_CM_CONFIG;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_LB_UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_LB_UPDATE_FINISHED;

@Component
public class StackLoadBalancerUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackLoadBalancerUpdateService.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    public void creatingLoadBalancerEntity(Stack stack) {
        clusterService.updateClusterStatusByStackId(stack.getId(), UPDATE_IN_PROGRESS);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.CREATE_LOAD_BALANCER_ENTITY);
        flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), STACK_LB_CREATE_ENTITY);
    }

    public void creatingCloudResources(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.CREATE_CLOUD_LOAD_BALANCER);
        flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), STACK_LB_CREATE_CLOUD_RESOURCE);
    }

    public void collectingMetadata(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.COLLECT_LOAD_BALANCER_METADATA);
        flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), STACK_LB_COLLECT_METADATA);
    }

    public void registeringPublicDns(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.LOAD_BALANCER_REGISTER_PUBLIC_DNS);
        flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), STACK_LB_REGISTER_PUBLIC_DNS);
    }

    public void registeringFreeIpaDns(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.LOAD_BALANCER_REGISTER_FREEIPA_DNS);
        flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), STACK_LB_REGISTER_FREEIPA_DNS);
    }

    public void updatingCmConfig(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.LOAD_BALANCER_UPDATE_CM_CONFIG);
        flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), STACK_LB_UPDATE_CM_CONFIG);
    }

    public void restartingCm(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.LOAD_BALANCER_RESTART_CM);
        flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), STACK_LB_RESTART_CM);
    }

    public void updateFinished(Stack stack) {
        clusterService.updateClusterStatusByStackId(stack.getId(), AVAILABLE);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.LOAD_BALANCER_UPDATE_FINISHED, "Load balancer has been created");
        flowMessageService.fireEventAndLog(stack.getId(), AVAILABLE.name(), STACK_LB_UPDATE_FINISHED);
    }

    public void updateClusterFailed(Long stackId, Exception exception) {
        clusterService.updateClusterStatusByStackId(stackId, AVAILABLE);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.LOAD_BALANCER_UPDATE_FAILED,
            "Load balancer creation failed failed " + exception.getMessage());
        flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), STACK_LB_UPDATE_FAILED);
    }
}
