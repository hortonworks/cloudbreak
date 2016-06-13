package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.InstanceStatus;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class ClusterUpscaleFlowService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpscaleFlowService.class);
    @Inject
    private StackService stackService;
    @Inject
    private ClusterService clusterService;
    @Inject
    private FlowMessageService flowMessageService;
    @Inject
    private EmailSenderService emailSenderService;
    @Inject
    private StackUpdater stackUpdater;
    @Inject
    private HostGroupService hostGroupService;
    @Inject
    private HostMetadataRepository hostMetadataRepository;

    public void upscalingAmbari(Stack stack) {
        clusterService.updateClusterStatusByStackId(stack.getId(), UPDATE_IN_PROGRESS, "Upscaling the cluster.");
        flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_SCALING_UP, UPDATE_IN_PROGRESS.name());
    }

    public void clusterUpscaleFinished(Stack stack, String hostgroupName) {
        int numOfFailedHosts = updateMetadata(stack, hostgroupName);
        boolean success = numOfFailedHosts == 0;
        if (success) {
            LOGGER.info("Cluster upscaled successfully");
            clusterService.updateClusterStatusByStackId(stack.getId(), AVAILABLE);
            flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_SCALED_UP, AVAILABLE.name());
            if (stack.getCluster().getEmailNeeded()) {
                emailSenderService.sendUpscaleSuccessEmail(stack.getCluster().getOwner(), stack.getAmbariIp(), stack.getCluster().getName());
                flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_NOTIFICATION_EMAIL, AVAILABLE.name());
            }
        } else {
            LOGGER.info("Cluster upscale failed. {} hosts failed to upscale", numOfFailedHosts);
            clusterService.updateClusterStatusByStackId(stack.getId(), UPDATE_FAILED);
            flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_SCALING_FAILED, UPDATE_FAILED.name(), "added",
                    String.format("Ambari upscale operation failed on %d node(s).", numOfFailedHosts));
        }
    }

    public void clusterUpscaleFailed(Stack stack, Exception errorDetails) {
        LOGGER.error("Error during Cluster upscale flow: " + errorDetails.getMessage(), errorDetails);
        clusterService.updateClusterStatusByStackId(stack.getId(), UPDATE_FAILED, errorDetails.getMessage());
        stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, String.format("New node(s) could not be added to the cluster: %s", errorDetails));
        flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_SCALING_FAILED, UPDATE_FAILED.name(), "added", errorDetails);
    }

    private int updateMetadata(Stack stack, String hostGroupName) {
        LOGGER.info("Start update metadata");
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), hostGroupName);
        Set<HostMetadata> hostMetadata = hostGroupService.findEmptyHostMetadataInHostGroup(hostGroup.getId());
        updateFailedHostMetaData(hostMetadata);
        int failedHosts = 0;
        for (HostMetadata hostMeta : hostMetadata) {
            if (!"BYOS".equals(stack.cloudPlatform()) && hostGroup.getConstraint().getInstanceGroup() != null) {
                stackService.updateMetaDataStatus(stack.getId(), hostMeta.getHostName(), InstanceStatus.REGISTERED);
            }
            hostGroupService.updateHostMetaDataStatus(hostMeta.getId(), HostMetadataState.HEALTHY);
            if (hostMeta.getHostMetadataState() == HostMetadataState.UNHEALTHY) {
                failedHosts++;
            }
        }
        return failedHosts;
    }

    private void updateFailedHostMetaData(Set<HostMetadata> hostMetadata) {
        List<String> upscaleHostNames = getHostNames(hostMetadata);
        Set<String> successHosts = new HashSet<>(upscaleHostNames);
        updateFailedHostMetaData(successHosts, hostMetadata);
    }

    private void updateFailedHostMetaData(Set<String> successHosts, Set<HostMetadata> hostMetadata) {
        for (HostMetadata metaData : hostMetadata) {
            if (!successHosts.contains(metaData.getHostName())) {
                metaData.setHostMetadataState(HostMetadataState.UNHEALTHY);
                hostMetadataRepository.save(metaData);
            }
        }
    }

    private List<String> getHostNames(Set<HostMetadata> hostMetadata) {
        return hostMetadata.stream().map(HostMetadata::getHostName).collect(Collectors.toList());
    }
}
