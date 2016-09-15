package com.sequenceiq.cloudbreak.core.cluster;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedExitCriteriaModel;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorType;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Service
public class AmbariClusterResetService {

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @Inject
    private HostOrchestratorResolver hostOrchestratorResolver;

    @Inject
    private StackRepository stackRepository;

    public void resetCluster(Long stackId) throws CloudbreakOrchestratorException {
        Stack stack = stackRepository.findOneWithLists(stackId);
        try {
            InstanceGroup gatewayInstanceGroup = stack.getGatewayInstanceGroup();
            InstanceMetaData gatewayInstance = gatewayInstanceGroup.getInstanceMetaData().iterator().next();
            GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack, gatewayInstance);
            OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(stack.getOrchestrator().getType());
            if (orchestratorType.hostOrchestrator()) {
                HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
                Set<String> gatewayFQDN = Collections.singleton(gatewayInstance.getDiscoveryFQDN());
                ExitCriteriaModel exitCriteriaModel = clusterDeletionBasedExitCriteriaModel(stack.getId(), stack.getCluster().getId());
                hostOrchestrator.resetAmbari(gatewayConfig, gatewayFQDN, StackUtil.collectNodes(stack), exitCriteriaModel);
            } else {
                throw new UnsupportedOperationException("ambari reset cluster works only with host orchestrator");
            }
        } catch (CloudbreakException e) {
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

}
