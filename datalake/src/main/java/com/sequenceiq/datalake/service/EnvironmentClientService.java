package com.sequenceiq.datalake.service;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionHandler;
import com.sequenceiq.common.api.backup.response.BackupResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.platformresource.CredentialPlatformResourceEndpoint;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformVmtypesResponse;

@Service
public class EnvironmentClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentClientService.class);

    @Inject
    private EnvironmentEndpoint environmentEndpoint;

    @Inject
    private CredentialEndpoint credentialEndpoint;

    @Inject
    private CredentialPlatformResourceEndpoint credentialPlatformResourceEndpoint;

    @Inject
    private WebApplicationExceptionHandler webApplicationExceptionHandler;

    public DetailedEnvironmentResponse getByName(String environment) {
        try {
            return environmentEndpoint.getByName(environment);
        } catch (WebApplicationException e) {
            throw webApplicationExceptionHandler.handleException(e);
        }
    }

    public DetailedEnvironmentResponse getByCrn(String environmentCrn) {
        try {
            return environmentEndpoint.getByCrn(environmentCrn);
        } catch (WebApplicationException e) {
            throw webApplicationExceptionHandler.handleException(e);
        }
    }

    public CredentialResponse getCredentialByCrn(String credentialCrn) {
        return credentialEndpoint.getByResourceCrn(credentialCrn);
    }

    /**
     * Get the backup location.
     * @param envCrn Environemnt CRN.
     * @return backuplocation configured for the environment, If not, returns the log location.
     */
    public String getBackupLocation(String environmentCrn, boolean isRangerRAZEnabled) {
        DetailedEnvironmentResponse detailedEnvironmentResponse = getByCrn(environmentCrn);
        BackupResponse backupResponse = detailedEnvironmentResponse.getBackup();
        TelemetryResponse telemetryResponse = detailedEnvironmentResponse.getTelemetry();
        if (backupResponse != null && backupResponse.getStorageLocation() != null) {
            LOGGER.info("Using the backup location to store the datalake backup");
            if (detailedEnvironmentResponse.getCloudPlatform() == "Azure" && isRangerRAZEnabled) {
                return getUpdatedAzureRAZLocation(backupResponse.getStorageLocation());
            }
            return backupResponse.getStorageLocation();
        } else if (telemetryResponse != null && telemetryResponse.getLogging() != null) {
            LOGGER.info("Backup location not configured. Using the log location to store the datalake backup");
            if (detailedEnvironmentResponse.getCloudPlatform() == "Azure" && isRangerRAZEnabled) {
                return getUpdatedAzureRAZLocation(telemetryResponse.getLogging().getStorageLocation());
            }
            return telemetryResponse.getLogging().getStorageLocation();
        } else {
            LOGGER.error("Could not identify the location to store the backup");
            throw new BadRequestException("Backup Location is empty. Datalake backup is not triggered.");
        }
    }

    public PlatformVmtypesResponse getVmTypesByCredential(String credentialCrn, String region, String platformVariant,
            CdpResourceType resourceType, String availabilityZone) {
        try {
            return credentialPlatformResourceEndpoint.getVmTypesByCredential(null, credentialCrn, region, platformVariant, availabilityZone,
                    resourceType);
        } catch (WebApplicationException e) {
            throw webApplicationExceptionHandler.handleException(e);
        }
    }

    /**
     * Check if an Azure storage location is a root directory. Below are the two kinds of root directories:
     * abfs://test@mydatalake.dfs.core.windows.net
     * abfs://test@mydatalake.dfs.core.windows.net/
     * @param locationInput The storage location.
     * @return The updated backup location.
     */
    public String getUpdatedAzureRAZLocation(String locationInput) {
        String locationAfterScheme = locationInput;
        locationAfterScheme = locationAfterScheme.replace("abfs://", "");
        String updatedLocation;
        //Example: test@mydatalake.dfs.core.windows.net
        if (!locationAfterScheme.contains("/")) {
            updatedLocation = locationInput + "/backups";
        } else {
            String[] locationAfterSchemeList = locationAfterScheme.split("/");
            //Example: test@mydatalake.dfs.core.windows.net/, so no slash needs to be added
            if (locationAfterSchemeList.length == 1) {
                updatedLocation = locationInput + "backups";
            } else {
                //This is not a root directory, so it needn't append anything.
                // Examples: test@mydatalake.dfs.core.windows.net/test; test@mydatalake.dfs.core.windows.net/test/
                updatedLocation = locationInput;
            }
        }
        return updatedLocation;
    }
}
