package com.sequenceiq.cloudbreak.orchestrator.metadata;

import java.util.List;

public interface OrchestratorMetadataProvider {

    OrchestratorMetadata getOrchestratorMetadata(Long stackId);

    byte[] getStoredStates(Long stackId);

    void storeNewState(Long stackId, byte[] newFullSaltState);

    List<String> getSaltStateDefinitionBaseFolders();

}
