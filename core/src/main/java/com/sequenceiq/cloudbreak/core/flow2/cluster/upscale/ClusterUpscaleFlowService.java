package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceStatus;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;

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
    private StackUpdater stackUpdater;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private HostMetadataRepository hostMetadataRepository;

    @Inject
    private StackUtil stackUtil;

    public void upscalingAmbari(long stackId) {
        clusterService.updateClusterStatusByStackId(stackId, UPDATE_IN_PROGRESS, "Upscaling the cluster.");
        flowMessageService.fireEventAndLog(stackId, Msg.AMBARI_CLUSTER_SCALING_UP, UPDATE_IN_PROGRESS.name());
    }

    public void clusterUpscaleFinished(StackView stackView, String hostgroupName) {
        LOGGER.info("Cluster upscaled successfully");
        clusterService.updateClusterStatusByStackId(stackView.getId(), AVAILABLE);
        flowMessageService.fireEventAndLog(stackView.getId(), Msg.AMBARI_CLUSTER_SCALED_UP, AVAILABLE.name());
    }

    public void clusterUpscaleFailed(long stackId, Exception errorDetails) {
        LOGGER.error("Error during Cluster upscale flow: " + errorDetails.getMessage(), errorDetails);
        clusterService.updateClusterStatusByStackId(stackId, UPDATE_FAILED, errorDetails.getMessage());
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.PROVISIONED,
                String.format("Error during cluster upscale: %s", errorDetails));
        flowMessageService.fireEventAndLog(stackId, Msg.AMBARI_CLUSTER_SCALING_FAILED, UPDATE_FAILED.name(), "upscale", errorDetails);
    }

    public void updateMetadata(StackView stackView, String hostGroupName) {
        LOGGER.info("Start update metadata");
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stackView.getClusterView().getId(), hostGroupName);
        Set<HostMetadata> hostMetadata = hostGroupService.findEmptyHostMetadataInHostGroup(hostGroup.getId());
        for (HostMetadata hostMeta : hostMetadata) {
            if (hostGroup.getInstanceGroup() != null) {
                stackService.updateMetaDataStatusIfFound(stackView.getId(), hostMeta.getHostName(), InstanceStatus.REGISTERED);
            }
            hostGroupService.updateHostMetaDataStatus(hostMeta.getId(), HostMetadataState.HEALTHY);
        }
    }

    private List<String> getHostNames(Collection<HostMetadata> hostMetadata) {
        return hostMetadata.stream().map(HostMetadata::getHostName).collect(Collectors.toList());
    }
}
