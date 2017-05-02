package com.sequenceiq.cloudbreak.core.flow2.cluster.repair;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.model.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;

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

    public void changePrimaryGatewayStarted(Stack stack) {
        clusterService.updateClusterStatusByStackId(stack.getId(), Status.UPDATE_IN_PROGRESS);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.CLUSTER_OPERATION, String.format("Changing gateway."));
    }

    public void primaryGatewayChanged(Stack stack, String newPrimaryGatewayFQDN) throws CloudbreakException {
        Set<InstanceMetaData> imds = instanceMetaDataRepository.findAllInStack(stack.getId());
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
            Stack updatedStack = stackService.getById(stack.getId());
            String gatewayIp = gatewayConfigService.getPrimaryGatewayIp(updatedStack);

            Cluster cluster = updatedStack.getCluster();
            cluster.setAmbariIp(gatewayIp);
            clusterRepository.save(cluster);
        } else {
            throw new CloudbreakException("Primary gateway change was not successful.");
        }
    }

    public void ambariServerStarted(Stack stack) {
        clusterService.updateClusterStatusByStackId(stack.getId(), Status.AVAILABLE);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.AVAILABLE, "Gateway succesfully changed.");
    }

    public void changePrimaryGatewayFailed(Stack stack, Exception exception) {
        clusterService.updateClusterStatusByStackId(stack.getId(), Status.UPDATE_FAILED);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.AVAILABLE, "Cluster could not be started: " + exception.getMessage());
    }
}
