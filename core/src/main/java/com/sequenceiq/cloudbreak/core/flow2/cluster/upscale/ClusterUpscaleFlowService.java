package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceStatus;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

@Component
public class ClusterUpscaleFlowService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpscaleFlowService.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private FlowMessageService flowMessageService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    public void upscalingAmbari(long stackId) {
        clusterService.updateClusterStatusByStackId(stackId, UPDATE_IN_PROGRESS, "Upscaling the cluster.");
        flowMessageService.fireEventAndLog(stackId, Msg.AMBARI_CLUSTER_SCALING_UP, UPDATE_IN_PROGRESS.name());
    }

    public void clusterUpscaleFinished(StackView stackView, String hostgroupName) {
        int numOfFailedHosts = updateMetadata(stackView, hostgroupName);
        boolean success = numOfFailedHosts == 0;
        if (success) {
            LOGGER.info("Cluster upscaled successfully");
            clusterService.updateClusterStatusByStackId(stackView.getId(), AVAILABLE);
            flowMessageService.fireEventAndLog(stackView.getId(), Msg.AMBARI_CLUSTER_SCALED_UP, AVAILABLE.name());
        } else {
            LOGGER.info("Cluster upscale failed. {} hosts failed to upscale", numOfFailedHosts);
            clusterService.updateClusterStatusByStackId(stackView.getId(), UPDATE_FAILED);
            flowMessageService.fireEventAndLog(stackView.getId(), Msg.AMBARI_CLUSTER_SCALING_FAILED, UPDATE_FAILED.name(), "added to",
                    String.format("Ambari upscale operation failed on %d node(s).", numOfFailedHosts));
        }
    }

    public void clusterUpscaleFailed(long stackId, Exception errorDetails) {
        LOGGER.error("Error during Cluster upscale flow: " + errorDetails.getMessage(), errorDetails);
        clusterService.updateClusterStatusByStackId(stackId, UPDATE_FAILED, errorDetails.getMessage());
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.PROVISIONED,
                String.format("New node(s) could not be added to the cluster: %s", errorDetails));
        flowMessageService.fireEventAndLog(stackId, Msg.AMBARI_CLUSTER_SCALING_FAILED, UPDATE_FAILED.name(), "added to", errorDetails);
    }

    private int updateMetadata(StackView stackView, String hostGroupName) {
        LOGGER.info("Start update metadata");
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stackView.getClusterView().getId(), hostGroupName);
        Set<InstanceMetaData> notDeletedInstanceMetaDataSet = hostGroup.getInstanceGroup().getNotDeletedInstanceMetaDataSet();
        return updateFailedHostMetaData(notDeletedInstanceMetaDataSet);
    }

    private int updateFailedHostMetaData(Collection<InstanceMetaData> instanceMetaData) {
        List<String> upscaleHostNames = getHostNames(instanceMetaData);
        Collection<String> successHosts = new HashSet<>(upscaleHostNames);
        return updateFailedHostMetaData(successHosts, instanceMetaData);
    }

    private int updateFailedHostMetaData(Collection<String> successHosts, Iterable<InstanceMetaData> instanceMetaDatas) {
        int failedHosts = 0;
        for (InstanceMetaData metaData : instanceMetaDatas) {
            if (!successHosts.contains(metaData.getDiscoveryFQDN())) {
                instanceMetaDataService.updateInstanceStatus(metaData, InstanceStatus.ORCHESTRATION_FAILED,
                        "Cluster upscale failed. Host does not have fqdn.");
                failedHosts++;
            }
        }
        return failedHosts;
    }

    private List<String> getHostNames(Collection<InstanceMetaData> instanceMetaData) {
        return instanceMetaData.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toList());
    }
}
