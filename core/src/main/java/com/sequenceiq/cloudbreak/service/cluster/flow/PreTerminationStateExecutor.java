package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Component
public class PreTerminationStateExecutor {
    @Inject
    private HostOrchestratorResolver hostOrchestratorResolver;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackUtil stackUtil;

    public void leaveAdDomain(Stack stack) throws CloudbreakException {
        if (stack.getCluster().isAdJoinable()) {
            try {
                HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
                GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
                hostOrchestrator.leaveAdDomain(gatewayConfig, stackUtil.collectNodes(stack), clusterDeletionBasedModel(stack.getId(),
                        stack.getCluster().getId()));
            } catch (CloudbreakOrchestratorFailedException e) {
                Set<Map.Entry<String, Collection<String>>> entries = e.getNodesWithErrors().asMap().entrySet();
                String errors;
                if (entries.isEmpty()) {
                    errors = e.getMessage();
                } else {
                    errors = entries.stream()
                            .map(entry -> entry.getKey() + ": " + entry.getValue())
                            .collect(Collectors.joining("\n"));
                }
                String message = "Leaving AD domain had some errors:\n" + errors;
                throw new CloudbreakException(message, e);
            }
        }
    }
}
