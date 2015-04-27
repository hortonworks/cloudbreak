package com.sequenceiq.cloudbreak.core.flow;

import java.util.Set;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;

public interface ContainerOrchestrator {

    ContainerOrchestratorClient bootstrap(Long stackId) throws CloudbreakException;

    void startRegistrator(ContainerOrchestratorClient client, Long stackId) throws CloudbreakException;
    void startAmbariServer(ContainerOrchestratorClient client, Long stackId) throws CloudbreakException;
    void startAmbariAgents(ContainerOrchestratorClient client, Long stackId) throws CloudbreakException;
    void startConsulWatches(ContainerOrchestratorClient client, Long stackId) throws CloudbreakException;

    ContainerOrchestratorTool type();

    void preSetupNewNode(Long stackId, InstanceGroup gateway, Set<String> instanceIds) throws CloudbreakException;

    void newHostgroupNodesSetup(Long stackId, Set<String> instanceIds, String hostGroup) throws CloudbreakException;

}
