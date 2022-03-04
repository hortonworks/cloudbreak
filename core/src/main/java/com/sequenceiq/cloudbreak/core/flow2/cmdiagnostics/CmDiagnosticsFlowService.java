package com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadataFilter;

@Service
public class CmDiagnosticsFlowService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmDiagnosticsFlowService.class);

    @Inject
    private StackService stackService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private TelemetryOrchestrator telemetryOrchestrator;

    public void init(Long stackId, Map<String, Object> parameters, Set<String> excludeHosts) throws CloudbreakOrchestratorFailedException {
        executeCmDiagnosticOperation(stackId, "init", excludeHosts,
                (gatewayConfigs, nodes, exitModel) -> telemetryOrchestrator.initDiagnosticCollection(gatewayConfigs, nodes, parameters, exitModel));
    }

    public void upload(Long stackId, Map<String, Object> parameters, Set<String> excludeHosts) throws CloudbreakOrchestratorFailedException {
        executeCmDiagnosticOperation(stackId, "upload", excludeHosts,
                (gatewayConfigs, nodes, exitModel) -> telemetryOrchestrator.uploadCollectedDiagnostics(gatewayConfigs, nodes, parameters, exitModel));
    }

    public void cleanup(Long stackId, Map<String, Object> parameters, Set<String> excludeHosts) throws CloudbreakOrchestratorFailedException {
        executeCmDiagnosticOperation(stackId, "cleanup", excludeHosts,
                (gatewayConfigs, nodes, exitModel) -> telemetryOrchestrator.cleanupCollectedDiagnostics(gatewayConfigs, nodes, parameters, exitModel));
    }

    private void executeCmDiagnosticOperation(Long stackId, String operation, Set<String> excludeHosts,
            CmDiagnosticsFlowOperation func) throws CloudbreakOrchestratorFailedException {
        LOGGER.debug("CM Diagnostics {} will be called only on the primary gateway address", operation);
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        String primaryGatewayIp = gatewayConfigService.getPrimaryGatewayIp(stack);
        Set<String> hosts = Set.of(primaryGatewayIp);
        OrchestratorMetadataFilter filter = OrchestratorMetadataFilter.Builder.newBuilder()
                .includeHosts(hosts)
                .exlcudeHosts(excludeHosts)
                .build();
        Set<Node> filteredNodes = filter.apply(stack.getAllNodes());
        ClusterDeletionBasedExitCriteriaModel exitModel = new ClusterDeletionBasedExitCriteriaModel(stackId, stack.getCluster().getId());
        if (filteredNodes.isEmpty()) {
            LOGGER.debug("CM Diagnostics {} has been skipped. (no target minions)", operation);
        } else {
            LOGGER.debug("CM Diagnostics operation '{}' has been started.", operation);
            func.apply(gatewayConfigs, filteredNodes, exitModel);
            LOGGER.debug("CM Diagnostics operation '{}' has been finished.", operation);
        }
    }
}
