package com.sequenceiq.datalake.cm;

import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.google.common.collect.Iterables;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.sdx.api.model.RangerCloudIdentitySyncState;
import com.sequenceiq.sdx.api.model.RangerCloudIdentitySyncStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RangerCloudIdentityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RangerCloudIdentityService.class);

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
            Optional<ApiCommand> apiCommand = clouderaManagerRangerUtil.setAzureCloudIdentityMapping(stackCrn, azureUserMapping);
            if (apiCommand.isEmpty()) {
                return newSyncStatus(RangerCloudIdentitySyncState.SUCCESS, "Sucessfully synced, no role refresh required");
            }
            return toRangerCloudIdentitySyncStatus(apiCommand.get());
        } catch (ApiException e) {
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

    public RangerCloudIdentitySyncStatus getRangerCloudIdentitySyncStatus(String envCrn, long commandId) {
        Optional<SdxCluster> sdxCluster = getSdxCluster(envCrn);
        return checkIfUnapplicable(sdxCluster).orElseGet(() -> getRangerCloudIdentitySyncStatus(sdxCluster.get(), commandId));
    }

    private RangerCloudIdentitySyncStatus getRangerCloudIdentitySyncStatus(SdxCluster sdxCluster, long commandId) {
        try {
            ApiCommand apiCommand = clouderaManagerRangerUtil.getApiCommand(sdxCluster.getStackCrn(), commandId);
            return toRangerCloudIdentitySyncStatus(apiCommand);
        } catch (ApiException e) {
            LOGGER.error("Encountered cloudera manager api exception", e);
            return newSyncStatus(RangerCloudIdentitySyncState.FAILED, "Encountered cloudera manager api exception");
        }
    }

    private RangerCloudIdentitySyncStatus toRangerCloudIdentitySyncStatus(ApiCommand apiCommand) {
        RangerCloudIdentitySyncStatus status = new RangerCloudIdentitySyncStatus();
        status.setCommandId(apiCommand.getId().longValue());
        status.setStatusReason(apiCommand.getResultMessage());
        status.setState(toRangerCloudIdentitySyncState(apiCommand));
        return status;
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
