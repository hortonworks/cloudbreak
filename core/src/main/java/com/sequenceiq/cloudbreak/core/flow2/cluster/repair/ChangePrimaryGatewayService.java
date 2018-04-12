package com.sequenceiq.cloudbreak.core.flow2.cluster.repair;

import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.model.InstanceMetadataType;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.Optional;
import java.util.Set;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;

@Component
public class ChangePrimaryGatewayService {
    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackService stackService;

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private AmbariClusterConnector ambariClusterConnector;

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private FlowMessageService flowMessageService;

    public void changePrimaryGatewayStarted(long stackId) {
        clusterService.updateClusterStatusByStackId(stackId, UPDATE_IN_PROGRESS);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CLUSTER_OPERATION, "Changing gateway.");
        flowMessageService.fireEventAndLog(stackId, Msg.AMBARI_CLUSTER_GATEWAY_CHANGE, UPDATE_IN_PROGRESS.name());
    }

    @Transactional
    public void primaryGatewayChanged(long stackId, String newPrimaryGatewayFQDN) throws CloudbreakException {
        Set<InstanceMetaData> imds = instanceMetaDataRepository.findNotTerminatedForStack(stackId);
        Optional<InstanceMetaData> formerPrimaryGateway =
                imds.stream().filter(imd -> imd.getInstanceMetadataType() == InstanceMetadataType.GATEWAY_PRIMARY).findFirst();
        Optional<InstanceMetaData> newPrimaryGateway =
                imds.stream().filter(imd -> imd.getDiscoveryFQDN().equals(newPrimaryGatewayFQDN)).findFirst();
        if (newPrimaryGateway.isPresent() && formerPrimaryGateway.isPresent()) {
            InstanceMetaData fpg = formerPrimaryGateway.get();
            fpg.setInstanceMetadataType(InstanceMetadataType.GATEWAY);
            instanceMetaDataRepository.save(fpg);
            InstanceMetaData npg = newPrimaryGateway.get();
            npg.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
            instanceMetaDataRepository.save(npg);
            Stack updatedStack = stackService.getByIdWithLists(stackId);
            String gatewayIp = gatewayConfigService.getPrimaryGatewayIp(updatedStack);

            Cluster cluster = updatedStack.getCluster();
            cluster.setAmbariIp(gatewayIp);
            clusterRepository.save(cluster);
        } else {
            throw new CloudbreakException("Primary gateway change was not successful.");
        }
    }

    public void ambariServerStarted(StackView stack) {
        clusterService.updateClusterStatusByStackId(stack.getId(), AVAILABLE);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.AVAILABLE, "Gateway succesfully changed.");
        flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_GATEWAY_CHANGED_SUCCESSFULLY, AVAILABLE.name(),
                stackUtil.extractAmbariIp(stack));
    }

    public void changePrimaryGatewayFailed(long stackId, Exception exception) {
        clusterService.updateClusterStatusByStackId(stackId, UPDATE_FAILED);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE, "Cluster could not be started: " + exception.getMessage());
        flowMessageService.fireEventAndLog(stackId, Msg.AMBARI_CLUSTER_GATEWAY_CHANGE_FAILED, UPDATE_FAILED.name(), exception.getMessage());
    }
}
