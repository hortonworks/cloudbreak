package com.sequenceiq.cloudbreak.service.salt;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.orchestrator.host.PartialStateUpdater;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadata;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadataProvider;
import com.sequenceiq.cloudbreak.util.CompressUtil;

@Service
public class PartialSaltStateUpdateService {
    private static final String SALT_DIRECTORY_PREFIX = "/salt/";

    private static final Logger LOGGER = LoggerFactory.getLogger(PartialSaltStateUpdateService.class);

    @Inject
    private OrchestratorMetadataProvider orchestratorMetadataProvider;

    @Inject
    private CompressUtil compressUtil;

    @Inject
    private PartialStateUpdater stateUpdateService;

    public void performSaltUpdate(Long stackId, List<String> componentNames) {
        List<String> saltStateDefinitions = orchestratorMetadataProvider.getSaltStateDefinitionBaseFolders();
        List<String> filteredSaltComponents = componentNames.stream().map(name -> SALT_DIRECTORY_PREFIX + name).collect(Collectors.toList());
        LOGGER.info("Performing partial salt update for Salt components: {}", filteredSaltComponents);
        try {
            byte[] currentSaltState = orchestratorMetadataProvider.getStoredStates(stackId);
            byte[] saltStateConfigs = compressUtil.generateCompressedOutputFromFolders(saltStateDefinitions, filteredSaltComponents);
            OrchestratorMetadata metadata = orchestratorMetadataProvider.getOrchestratorMetadata(stackId);

            if (currentSaltState == null) {
                stateUpdateService.updatePartialSaltDefinition(saltStateConfigs, componentNames, metadata.getGatewayConfigs(), metadata.getExitCriteriaModel());
            } else {
                boolean saltStateContentMatches = compressUtil.compareCompressedContent(currentSaltState, saltStateConfigs, filteredSaltComponents);
                if (!saltStateContentMatches) {
                    LOGGER.info("Salt state for components {} on cluster does not match state on CB, performing update.", filteredSaltComponents);
                    stateUpdateService.updatePartialSaltDefinition(saltStateConfigs, componentNames, metadata.getGatewayConfigs(),
                            metadata.getExitCriteriaModel());
                    byte[] newFullSaltState = compressUtil.updateCompressedOutputFolders(saltStateDefinitions, filteredSaltComponents, currentSaltState);
                    orchestratorMetadataProvider.storeNewState(stackId, newFullSaltState);
                } else {
                    LOGGER.info("Salt state for components {} on cluster matches state on CB, skipping update.", filteredSaltComponents);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Failed to update the Salt state for components " + filteredSaltComponents, ex);
            throw new CloudbreakServiceException(ex.getMessage(), ex);
        }
    }
}
