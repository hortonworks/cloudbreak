package com.sequenceiq.cloudbreak.core.bootstrap.service.host;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedExitCriteriaModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;

@Component
public class ClusterHostRunner {

    @Inject
    private StackRepository stackRepository;
    @Inject
    private HostOrchestratorResolver hostOrchestratorResolver;
    @Inject
    private TlsSecurityService tlsSecurityService;

    public void runAmbariServices(ProvisioningContext context) throws CloudbreakException {
        try {
            Stack stack = stackRepository.findOneWithLists(context.getStackId());
            Set<String> agents = initializeAmbariAgentServices(stack);
            HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
            InstanceGroup gateway = stack.getGatewayInstanceGroup();
            InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();
            GatewayConfig gatewayConfig = tlsSecurityService.buildGatewayConfig(stack.getId(), gatewayInstance.getPublicIpWrapper(),
                    gatewayInstance.getPrivateIp());
            OrchestrationCredential credential = new OrchestrationCredential(stack.getOrchestrator().getApiEndpoint(), new HashMap<String, Object>());
            hostOrchestrator.runService(gatewayConfig, agents, credential, clusterDeletionBasedExitCriteriaModel(stack.getId(), stack.getCluster().getId()));
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new CancellationException(e.getMessage());
        } catch (CloudbreakOrchestratorException e) {
            throw new CloudbreakException(e);
        }
    }

    private Set<String> initializeAmbariAgentServices(Stack stack)
            throws CloudbreakException, CloudbreakOrchestratorException {
        Set<String> agents = new HashSet<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            for (InstanceMetaData instanceMetaData : instanceGroup.getInstanceMetaData()) {
                agents.add(instanceMetaData.getPrivateIp());
            }
        }
        return agents;
    }

}
