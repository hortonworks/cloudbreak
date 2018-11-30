package com.sequenceiq.cloudbreak.core.flow2.cluster.repair.master.ha;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
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
    private ClusterService clusterService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private FlowMessageService flowMessageService;

    @Inject
    private TransactionService transactionService;

    public void changePrimaryGatewayStarted(long stackId) {
        clusterService.updateClusterStatusByStackId(stackId, UPDATE_IN_PROGRESS);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CLUSTER_OPERATION, "Changing gateway.");
        flowMessageService.fireEventAndLog(stackId, Msg.AMBARI_CLUSTER_GATEWAY_CHANGE, UPDATE_IN_PROGRESS.name());
    }

    public void primaryGatewayChanged(long stackId, String newPrimaryGatewayFQDN) throws CloudbreakException, TransactionExecutionException {
        Set<InstanceMetaData> imds = instanceMetaDataRepository.findNotTerminatedForStack(stackId);
        Optional<InstanceMetaData> formerPrimaryGateway =
                imds.stream().filter(imd -> imd.getInstanceMetadataType() == InstanceMetadataType.GATEWAY_PRIMARY).findFirst();
        Optional<InstanceMetaData> newPrimaryGateway =
                imds.stream().filter(imd -> imd.getDiscoveryFQDN().equals(newPrimaryGatewayFQDN)).findFirst();
        if (newPrimaryGateway.isPresent() && formerPrimaryGateway.isPresent()) {
            InstanceMetaData fpg = formerPrimaryGateway.get();
            fpg.setInstanceMetadataType(InstanceMetadataType.GATEWAY);
            fpg.setAmbariServer(Boolean.FALSE);
            transactionService.required(() -> {
                instanceMetaDataRepository.save(fpg);
                InstanceMetaData npg = newPrimaryGateway.get();
                npg.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
                npg.setAmbariServer(Boolean.TRUE);
                instanceMetaDataRepository.save(npg);
                Stack updatedStack = stackService.getByIdWithListsInTransaction(stackId);
                String gatewayIp = gatewayConfigService.getPrimaryGatewayIp(updatedStack);

                Cluster cluster = updatedStack.getCluster();
                cluster.setAmbariIp(gatewayIp);
                clusterService.save(cluster);
                return null;
            });
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
