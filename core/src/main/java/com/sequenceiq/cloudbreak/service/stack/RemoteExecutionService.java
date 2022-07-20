package com.sequenceiq.cloudbreak.service.stack;

import java.util.Map;
import java.util.Set;

import javax.ws.rs.ServiceUnavailableException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadata;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadataFilter;
import com.sequenceiq.cloudbreak.service.orchestrator.OrchestratorService;
import com.sequenceiq.common.api.command.RemoteCommandsExecutionRequest;
import com.sequenceiq.common.api.command.RemoteCommandsExecutionResponse;

@Component
public class RemoteExecutionService {

    private final boolean remoteCommandExecution;

    private final StackService stackService;

    private final HostOrchestrator hostOrchestrator;

    private final OrchestratorService orchestratorService;

    public RemoteExecutionService(StackService stackService, HostOrchestrator hostOrchestrator, OrchestratorService orchestratorService,
            @Value("${testing.remote-command-execution:false}") boolean remoteCommandExecution) {
        this.stackService = stackService;
        this.hostOrchestrator = hostOrchestrator;
        this.orchestratorService = orchestratorService;
        this.remoteCommandExecution = remoteCommandExecution;
    }

    public RemoteCommandsExecutionResponse remoteExec(String resourceCrn, RemoteCommandsExecutionRequest req) {
        if (!remoteCommandExecution) {
            throw new ServiceUnavailableException("Remote command execution is not supported!");
        }
        Stack stack = stackService.getByCrn(resourceCrn);
        OrchestratorMetadata metadata = orchestratorService.getOrchestratorMetadata(stack.getId());
        OrchestratorMetadataFilter filter = OrchestratorMetadataFilter.Builder.newBuilder()
                .includeHosts(req.getHosts())
                .includeHostGroups(req.getHostGroups())
                .build();
        Set<Node> nodes = filter.apply(metadata.getNodes());
        try {
            Map<String, String> results = hostOrchestrator.runCommandOnHosts(metadata.getGatewayConfigs(), nodes, req.getCommand());
            RemoteCommandsExecutionResponse response = new RemoteCommandsExecutionResponse();
            response.setResults(results);
            return response;
        } catch (CloudbreakOrchestratorFailedException orchestratorFailedException) {
            throw new CloudbreakServiceException(orchestratorFailedException);
        }
    }
}
