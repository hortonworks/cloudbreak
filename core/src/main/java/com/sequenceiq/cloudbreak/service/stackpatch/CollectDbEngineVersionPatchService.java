package com.sequenceiq.cloudbreak.service.stackpatch;

import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.service.database.EmbeddedDbVersionCollector;
import com.sequenceiq.cloudbreak.service.datalake.SdxClientService;
import com.sequenceiq.cloudbreak.service.externaldatabase.ExternalDbVersionCollector;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@Service
public class CollectDbEngineVersionPatchService extends ExistingStackPatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectDbEngineVersionPatchService.class);

    @Inject
    private ExternalDbVersionCollector externalDbVersionCollector;

    @Inject
    private EmbeddedDbVersionCollector embeddedDbVersionCollector;

    @Inject
    private StackService stackService;

    @Inject
    private SdxClientService sdxClientService;

    @Override
    public StackPatchType getStackPatchType() {
        return StackPatchType.COLLECT_DB_ENGINE_VERSION;
    }

    @Override
    public boolean isAffected(Stack stack) {
        return StringUtils.isBlank(stack.getExternalDatabaseEngineVersion());
    }

    @Override
    boolean doApply(Stack stack) throws ExistingStackPatchApplyException {
        if (stack.isAvailable() || StringUtils.isNotBlank(stack.getCluster().getDatabaseServerCrn())) {
            Optional<String> dbEngineVersion = collectDbEngineVersion(stack);
            LOGGER.info("Collected DB engine version: {}", dbEngineVersion);
            if (dbEngineVersion.isPresent()) {
                updateDbEngineVersion(stack, dbEngineVersion.get());
                return true;
            } else {
                LOGGER.debug("DB engine version collection was unsuccessful");
                return false;
            }
        } else {
            LOGGER.info("Stack state [{}] is not available and doesn't have external database, skipping engine version collection", stack.getStackStatus());
            return false;
        }
    }

    private void updateDbEngineVersion(Stack stack, String dbEngineVersion) {
        if (stack.isDatalake()) {
            LOGGER.info("Updating DB engine version in datalake service with crn [{}] to [{}]", stack.getResourceCrn(), dbEngineVersion);
            try {
                sdxClientService.updateDatabaseEngineVersion(stack.getResourceCrn(), dbEngineVersion);
            } catch (NotFoundException e) {
                tryToFindAndUpdateDatalakeByNameIfCrnIsDifferent(stack, dbEngineVersion);
            }
        }
        stackService.updateExternalDatabaseEngineVersion(stack.getId(), dbEngineVersion);
    }

    private void tryToFindAndUpdateDatalakeByNameIfCrnIsDifferent(Stack stack, String dbEngineVersion) {
        LOGGER.info("Datalake not found by crn [{}], try to find by name [{}]", stack.getResourceCrn(), stack.getName());
        Optional<SdxClusterResponse> dlByEnvCrn = sdxClientService.getByEnvironmentCrnInernal(stack.getEnvironmentCrn()).stream()
                .filter(dl -> stack.getName().equals(dl.getName()))
                .findFirst();
        if (dlByEnvCrn.isPresent()) {
            LOGGER.info("Datalake found by name with crn: [{}]", dlByEnvCrn.get().getCrn());
            sdxClientService.updateDatabaseEngineVersion(dlByEnvCrn.get().getCrn(), dbEngineVersion);
        } else {
            throw new com.sequenceiq.cloudbreak.common.exception.NotFoundException(String.format("Cannot found datalake with name %s in env %s",
                    stack.getName(), stack.getEnvironmentCrn()));
        }
    }

    private Optional<String> collectDbEngineVersion(Stack stack) {
        if (StringUtils.isNotBlank(stack.getCluster().getDatabaseServerCrn())) {
            return collectVersionForExternalDb(stack);
        } else {
            return collectVersionForEmbedded(stack);
        }
    }

    private Optional<String> collectVersionForExternalDb(Stack stack) {
        LOGGER.debug("Collect DB engine version for external database");
        return externalDbVersionCollector.collectDbVersion(stack.getCluster().getDatabaseServerCrn());
    }

    private Optional<String> collectVersionForEmbedded(Stack stack) {
        try {
            LOGGER.debug("Collect DB engine version for embedded database");
            return embeddedDbVersionCollector.collectDbVersion(stack);
        } catch (CloudbreakOrchestratorFailedException e) {
            LOGGER.error("Couldn't collect embedded db version", e);
            return Optional.empty();
        }
    }
}
