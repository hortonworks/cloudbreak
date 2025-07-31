package com.sequenceiq.datalake.cm;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.google.common.collect.Iterables;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.sdx.api.model.RangerCloudIdentitySyncState;
import com.sequenceiq.sdx.api.model.RangerCloudIdentitySyncStatus;

@Service
public class RangerCloudIdentityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RangerCloudIdentityService.class);

    private static final String ENCOUNTERED_CLOUDERA_MANAGER_API_EXCEPTION = "Encountered cloudera manager api exception";

    @Inject
    private ClouderaManagerRangerUtil clouderaManagerRangerUtil;

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxStatusService sdxStatusService;

    private RangerCloudIdentitySyncStatus newSyncStatus(RangerCloudIdentitySyncState state, String message) {
        RangerCloudIdentitySyncStatus status = new RangerCloudIdentitySyncStatus();
        status.setState(state);
        status.setStatusReason(message);
        return status;
    }

    private RangerCloudIdentitySyncStatus setAzureCloudIdentityMapping(String envCrn, SdxCluster sdxCluster, Map<String, String> azureUserMapping) {
        String stackCrn = sdxCluster.getStackCrn();
        LOGGER.info("Updating azure cloud id mappings for envCrn = {}, datalake stack crn = {}", envCrn, stackCrn);
        try {
            if (!clouderaManagerRangerUtil.isCloudIdMappingSupported(sdxCluster.getStackCrn())) {
                return newSyncStatus(RangerCloudIdentitySyncState.NOT_APPLICABLE,
                        "The datalake does not support cloud identity sync. Sync request is ignored.");
            }
            List<ApiCommand> apiCommands = clouderaManagerRangerUtil.setAzureCloudIdentityMapping(stackCrn, azureUserMapping);
            if (apiCommands.isEmpty()) {
                return newSyncStatus(RangerCloudIdentitySyncState.SUCCESS, "Successfully synced, no role refresh required");
            } else {
                return toRangerCloudIdentitySyncStatus(apiCommands);
            }
        } catch (CloudbreakServiceException | ApiException e) {
            LOGGER.error("Encountered api exception", e);
            return newSyncStatus(RangerCloudIdentitySyncState.FAILED, "Encountered cloudera manager api exception");
        }
    }

    private Optional<SdxCluster> getSdxCluster(String envCrn) {
        List<SdxCluster> sdxClusters = sdxService.listSdxByEnvCrn(envCrn);
        if (sdxClusters.isEmpty()) {
            return Optional.empty();
        } else if (sdxClusters.size() > 1) {
            LOGGER.error("Multiple datalakes per environment not supported, environmentCrn = {}", envCrn);
            throw new IllegalStateException("Multiple datalakes per environment not supported");
        }
        return Optional.of(Iterables.getOnlyElement(sdxClusters));
    }

    private boolean isDatalakeRunning(SdxCluster sdxCluster) {
        SdxStatusEntity sdxStatusEntity = sdxStatusService.getActualStatusForSdx(sdxCluster);
        LOGGER.debug("SDX status = {}", sdxStatusEntity.getStatus());
        return sdxStatusEntity.getStatus().equals(DatalakeStatusEnum.RUNNING);
    }

    public Optional<RangerCloudIdentitySyncStatus> checkIfUnapplicable(Optional<SdxCluster> sdxCluster) {
        if (sdxCluster.isEmpty()) {
            return Optional.of(newSyncStatus(RangerCloudIdentitySyncState.NOT_APPLICABLE, "No datalakes associated with the environment."));
        } else if (!isDatalakeRunning(sdxCluster.get())) {
            return Optional.of(newSyncStatus(RangerCloudIdentitySyncState.NOT_APPLICABLE, "Datalake is not running for the environment."));
        } else {
            return Optional.empty();
        }
    }

    public RangerCloudIdentitySyncStatus setAzureCloudIdentityMapping(String envCrn, Map<String, String> azureUserMapping) {
        Optional<SdxCluster> sdxCluster = getSdxCluster(envCrn);
        return checkIfUnapplicable(sdxCluster).orElseGet(() -> setAzureCloudIdentityMapping(envCrn, sdxCluster.get(), azureUserMapping));
    }

    public RangerCloudIdentitySyncStatus getRangerCloudIdentitySyncStatus(String envCrn, List<Long> commandIds) {
        Optional<SdxCluster> sdxCluster = getSdxCluster(envCrn);
        return checkIfUnapplicable(sdxCluster).orElseGet(() -> getRangerCloudIdentitySyncStatus(sdxCluster.get(), commandIds));
    }

    private RangerCloudIdentitySyncStatus getRangerCloudIdentitySyncStatus(SdxCluster sdxCluster, List<Long> commandIds) {
        try {
            List<ApiCommand> apiCommands = commandIds.stream().map(commandId -> {
                try {
                    return clouderaManagerRangerUtil.getApiCommand(sdxCluster.getStackCrn(), commandId);
                } catch (ApiException e) {
                    throw new CloudbreakServiceException(e);
                }
            }).toList();
            return toRangerCloudIdentitySyncStatus(apiCommands);
        } catch (CloudbreakServiceException e) {
            LOGGER.error(ENCOUNTERED_CLOUDERA_MANAGER_API_EXCEPTION, e);
            return newSyncStatus(RangerCloudIdentitySyncState.FAILED, ENCOUNTERED_CLOUDERA_MANAGER_API_EXCEPTION);
        }
    }

    private RangerCloudIdentitySyncStatus toRangerCloudIdentitySyncStatus(List<ApiCommand> apiCommands) {
        RangerCloudIdentitySyncStatus status = new RangerCloudIdentitySyncStatus();
        status.setCommandId(apiCommands.getFirst().getId().longValue());
        status.setCommandIds(apiCommands.stream().map(ApiCommand::getId).mapToLong(BigDecimal::longValue).boxed().toList());
        status.setStatusReason(collectReasonForCommands(apiCommands));
        status.setState(toRangerCloudIdentitySyncState(apiCommands));
        return status;
    }

    private String collectReasonForCommands(List<ApiCommand> apiCommands) {
        return apiCommands.stream()
                .map(command -> String.format("Command [%s] reason: %s", command.getId(), command.getResultMessage()))
                .collect(Collectors.joining("; "));
    }

    private RangerCloudIdentitySyncState toRangerCloudIdentitySyncState(List<ApiCommand> apiCommands) {
        List<RangerCloudIdentitySyncState> syncStates = apiCommands.stream().map(this::toRangerCloudIdentitySyncState).toList();
        if (syncStates.stream().anyMatch(RangerCloudIdentitySyncState.FAILED::equals)) {
            return RangerCloudIdentitySyncState.FAILED;
        } else if (syncStates.stream().anyMatch(RangerCloudIdentitySyncState.ACTIVE::equals)) {
            return RangerCloudIdentitySyncState.ACTIVE;
        } else {
            return RangerCloudIdentitySyncState.SUCCESS;
        }
    }

    private RangerCloudIdentitySyncState toRangerCloudIdentitySyncState(ApiCommand apiCommand) {
        if (apiCommand.getActive()) {
            return RangerCloudIdentitySyncState.ACTIVE;
        } else if (apiCommand.getSuccess()) {
            return RangerCloudIdentitySyncState.SUCCESS;
        } else {
            return RangerCloudIdentitySyncState.FAILED;
        }
    }

}
