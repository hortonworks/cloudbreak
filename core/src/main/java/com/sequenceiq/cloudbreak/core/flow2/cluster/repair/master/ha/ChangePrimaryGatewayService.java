package com.sequenceiq.cloudbreak.core.flow2.cluster.repair.master.ha;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_GATEWAY_CHANGE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_GATEWAY_CHANGED_SUCCESSFULLY;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_GATEWAY_CHANGE_FAILED;

import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.publicendpoint.ClusterPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;

@Component
public class ChangePrimaryGatewayService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangePrimaryGatewayService.class);

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private ClusterPublicEndpointManagementService clusterPublicEndpointManagementService;

    public void changePrimaryGatewayStarted(long stackId) {
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CLUSTER_OPERATION, "Changing gateway.");
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), CLUSTER_GATEWAY_CHANGE);
    }

    public void primaryGatewayChanged(long stackId, String newPrimaryGatewayFQDN) throws CloudbreakException, TransactionExecutionException {
        LOGGER.info("Update primary gateway ip");
        Set<InstanceMetaData> imds = instanceMetaDataService.findNotTerminatedAndNotZombieForStack(stackId);
        Optional<InstanceMetaData> formerPrimaryGateway = imds.stream()
                .filter(imd -> imd.getInstanceMetadataType() == InstanceMetadataType.GATEWAY_PRIMARY)
                .findFirst();
        Optional<InstanceMetaData> newPrimaryGateway = imds.stream()
                .filter(imd -> imd.getDiscoveryFQDN() != null && imd.getDiscoveryFQDN().equals(newPrimaryGatewayFQDN))
                .findFirst();
        if (newPrimaryGateway.isPresent() && formerPrimaryGateway.isPresent()) {
            InstanceMetaData fpg = formerPrimaryGateway.get();
            fpg.setInstanceMetadataType(InstanceMetadataType.GATEWAY);
            fpg.setServer(Boolean.FALSE);
            transactionService.required(() -> {
                instanceMetaDataService.save(fpg);
                InstanceMetaData npg = newPrimaryGateway.get();
                npg.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
                npg.setServer(Boolean.TRUE);
                instanceMetaDataService.save(npg);
                StackDto updatedStack = stackDtoService.getById(stackId);
                String gatewayIp = gatewayConfigService.getPrimaryGatewayIp(updatedStack);

                clusterService.updateClusterManagerIp(updatedStack.getCluster().getId(), gatewayIp);
                LOGGER.info("Primary gateway IP has been updated to: '{}'", gatewayIp);
                clusterPublicEndpointManagementService.changeGateway(updatedStack);
                return null;
            });
        } else {
            throw new CloudbreakException("Primary gateway change was not successful.");
        }
    }

    public void ambariServerStarted(StackView stack, ClusterView cluster, InstanceMetadataView primaryGatewayInstanceMetadata) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.AVAILABLE, "Gateway successfully changed.");
        String clusterManagerIp = stackUtil.extractClusterManagerIp(cluster, primaryGatewayInstanceMetadata);
        flowMessageService.fireEventAndLog(stack.getId(), AVAILABLE.name(), CLUSTER_GATEWAY_CHANGED_SUCCESSFULLY, clusterManagerIp);
    }

    public void changePrimaryGatewayFailed(long stackId, Exception exception) {
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.PRIMARY_GATEWAY_CHANGE_FAILED,
                "Cluster could not be started: " + exception.getMessage());
        flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), CLUSTER_GATEWAY_CHANGE_FAILED, exception.getMessage());
    }
}
