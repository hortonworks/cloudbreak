package com.sequenceiq.datalake.service;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.common.api.backup.response.BackupResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionHandler;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class EnvironmentClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentClientService.class);

    @Inject
    private EnvironmentEndpoint environmentEndpoint;

    @Inject
    private CredentialEndpoint credentialEndpoint;

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
    public String getBackupLocation(String environmentCrn) {
        DetailedEnvironmentResponse detailedEnvironmentResponse = getByCrn(environmentCrn);
        BackupResponse backupResponse = detailedEnvironmentResponse.getBackup();
        TelemetryResponse telemetryResponse = detailedEnvironmentResponse.getTelemetry();
        if (backupResponse != null && backupResponse.getStorageLocation() != null) {
            LOGGER.info("Using the backup location to store the datalake backup");
            return backupResponse.getStorageLocation();
        } else if (telemetryResponse != null && telemetryResponse.getLogging() != null) {
            LOGGER.info("Backup location not configured. Using the log location to store the datalake backup");
            return telemetryResponse.getLogging().getStorageLocation();
        } else {
            LOGGER.error("Could not identify the location to store the backup");
            throw new BadRequestException("Backup Location is empty. Datalake backup is not triggered.");
        }
    }
}
