package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_ROOT_VOLUME_INCREASED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_ROOT_VOLUME_INCREASING;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALED_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALED_INSTANCES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALE_RESTARTED_INSTANCES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALE_RESTARTING_INSTANCES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALE_RESTART_INSTANCES_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALE_STOPPED_INSTANCES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALE_STOPPING_INSTANCES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALE_STOP_INSTANCES_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALING_INSTANCES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALING_INSTANCES_FAILED;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

@Component
public class RollingVerticalScaleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RollingVerticalScaleService.class);

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ClusterService clusterService;

    public void stopInstances(Long stackId, List<String> instanceIds, String group) {
        clusterService.updateClusterStatusByStackId(stackId, DetailedStackStatus.CLUSTER_VERTICALSCALE_IN_PROGRESS);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(),
                CLUSTER_VERTICALSCALE_STOPPING_INSTANCES, group, String.join(", ", instanceIds));
    }

    public void finishStopInstances(Long stackId, List<String> stoppedInstances, String group) {
        if (!stoppedInstances.isEmpty()) {
            instanceMetaDataService.updateStatus(stackId, stoppedInstances, InstanceStatus.STOPPED);
            flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(),
                    CLUSTER_VERTICALSCALE_STOPPED_INSTANCES, group, String.join(", ", stoppedInstances));
        }
    }

    public void failedToStopInstance(Long stackId, List<String> failedToStopCloudInstance, String group, String message) {
        if (!failedToStopCloudInstance.isEmpty()) {
            flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(),
                    CLUSTER_VERTICALSCALE_STOP_INSTANCES_FAILED, group, String.join(",", failedToStopCloudInstance), message);
        }
    }

    public void verticalScaleInstances(Long stackId, List<String> instanceIds, StackVerticalScaleV4Request payload) {
        if (!instanceIds.isEmpty()) {
            if (payload.getTemplate().getInstanceType() != null) {
                flowMessageService.fireEventAndLog(stackId,
                        Status.UPDATE_IN_PROGRESS.name(),
                        CLUSTER_VERTICALSCALING_INSTANCES,
                        payload.getGroup(),
                        String.join(", ", instanceIds));
            }
            if (payload.getTemplate().getRootVolume() != null && payload.getTemplate().getRootVolume().getSize() != null) {
                flowMessageService.fireEventAndLog(stackId,
                        Status.UPDATE_IN_PROGRESS.name(),
                        CLUSTER_ROOT_VOLUME_INCREASING,
                        payload.getTemplate().getRootVolume().getSize().toString(),
                        payload.getGroup());
            }
        }
    }

    public void finishVerticalScaleInstances(Long stackId, List<String> instanceIds, StackVerticalScaleV4Request payload) {
        if (!instanceIds.isEmpty()) {
            if (payload.getTemplate().getInstanceType() != null) {
                flowMessageService.fireEventAndLog(stackId,
                        Status.STOPPED.name(),
                        CLUSTER_VERTICALSCALED_INSTANCES,
                        payload.getGroup(),
                        String.join(", ", instanceIds));
            }
            if (payload.getTemplate().getRootVolume() != null && payload.getTemplate().getRootVolume().getSize() != null) {
                flowMessageService.fireEventAndLog(stackId,
                        Status.UPDATE_IN_PROGRESS.name(),
                        CLUSTER_ROOT_VOLUME_INCREASED,
                        payload.getTemplate().getRootVolume().getSize().toString(),
                        payload.getGroup());
            }
        }
    }

    public void failedVerticalScaleInstances(Long stackId, List<String> instanceIds, StackVerticalScaleV4Request payload, String message) {
        if (!instanceIds.isEmpty()) {
            if (payload.getTemplate().getInstanceType() != null) {
                flowMessageService.fireEventAndLog(stackId,
                        UPDATE_IN_PROGRESS.name(),
                        CLUSTER_VERTICALSCALING_INSTANCES_FAILED,
                        payload.getGroup(),
                        String.join(", ", instanceIds),
                        message);
            }
        }
    }

    public void startInstances(Long stackId, List<String> instanceIds, String group) {
        instanceMetaDataService.updateStatus(stackId, instanceIds, InstanceStatus.RESTARTING);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(),
                CLUSTER_VERTICALSCALE_RESTARTING_INSTANCES, group, String.join(", ", instanceIds));
    }

    public void finishStartInstances(Long stackId, List<String> instanceIds, String group) {
        instanceMetaDataService.updateStatus(stackId, instanceIds, InstanceStatus.SERVICES_RUNNING);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(),
                CLUSTER_VERTICALSCALE_RESTARTED_INSTANCES, group, String.join(", ", instanceIds));
    }

    public void failedStartInstances(Long stackId, List<String> instanceIds, String group, String message) {
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(),
                CLUSTER_VERTICALSCALE_RESTART_INSTANCES_FAILED, group, String.join(", ", instanceIds), message);
    }

    public void finishVerticalScale(Long stackId, List<String> instanceIds, String group, String previousInstanceType, String targetInstanceType) {
        clusterService.updateClusterStatusByStackId(stackId, DetailedStackStatus.CLUSTER_VERTICALSCALE_COMPLETE);
        flowMessageService.fireEventAndLog(stackId, AVAILABLE.name(), CLUSTER_VERTICALSCALED, group, previousInstanceType, targetInstanceType);
    }

    public void failedVerticalScale(Long stackId, List<String> instanceIds, String message) {
        clusterService.updateClusterStatusByStackId(stackId, DetailedStackStatus.CLUSTER_VERTICALSCALE_FAILED, message);
        flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(),
                CLUSTER_VERTICALSCALED_FAILED, message, String.join(", ", instanceIds));
    }
}
