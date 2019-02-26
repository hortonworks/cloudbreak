package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
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

    public void runPreteraminationTasks(Stack stack) throws CloudbreakException {
        leaveDomains(stack);
    }

    private void leaveDomains(Stack stack) throws CloudbreakException {
        Cluster cluster = stack.getCluster();
        if (cluster.isAdJoinable() || cluster.isIpaJoinable()) {
            try {
                HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
                GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
                if (cluster.isAdJoinable()) {
                    hostOrchestrator.leaveDomain(gatewayConfig, stackUtil.collectNodes(stack), "ad_member", "ad_leave",
                            clusterDeletionBasedModel(stack.getId(), cluster.getId()));
                } else if (cluster.isIpaJoinable()) {
                    hostOrchestrator.leaveDomain(gatewayConfig, stackUtil.collectNodes(stack), "ipa_member", "ipa_leave",
                            clusterDeletionBasedModel(stack.getId(), cluster.getId()));
                }
            } catch (CloudbreakOrchestratorFailedException e) {
                Set<Entry<String, Collection<String>>> entries = e.getNodesWithErrors().asMap().entrySet();
                String errors;
                errors = entries.isEmpty() ? e.getMessage() : entries.stream()
                        .map(entry -> entry.getKey() + ": " + entry.getValue())
                        .collect(Collectors.joining("\n"));
                String message = "Leaving AD domain had some errors:\n" + errors;
                throw new CloudbreakException(message, e);
            }
        }
    }
}
