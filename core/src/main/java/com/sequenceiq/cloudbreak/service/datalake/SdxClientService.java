package com.sequenceiq.cloudbreak.service.datalake;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.sdx.api.endpoint.SdxBackupEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxInternalEndpoint;
import com.sequenceiq.sdx.api.model.SdxBackupRestoreSettingsResponse;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@Service
public class SdxClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxClientService.class);

    @Inject
    private SdxEndpoint sdxEndpoint;

    @Inject
    private SdxInternalEndpoint internalEndpoint;

    @Inject
    private SdxBackupEndpoint sdxBackupEndpoint;

    public List<SdxClusterResponse> getByEnvironmentCrn(String environmentCrn) {
        try {
            return sdxEndpoint.getByEnvCrn(environmentCrn, false);
        } catch (WebApplicationException | ProcessingException | IllegalStateException e) {
            LOGGER.error(String.format("Failed to get datalake clusters for environment %s", environmentCrn), e);
            return new ArrayList<>();
        }
    }

    public SdxClusterResponse getByCrnInternal(String crn) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> sdxEndpoint.getByCrn(crn));
    }

    public void updateDatabaseEngineVersion(String crn, String databaseEngineVersion) {
        ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> internalEndpoint.updateDbEngineVersion(crn, databaseEngineVersion));
    }

    public SdxBackupRestoreSettingsResponse getBackupRestoreSettings(String crn) {
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> sdxBackupEndpoint.internalGetBackupRestoreSettings(crn));
        } catch (NotFoundException e) {
            return null;
        }
    }
}
