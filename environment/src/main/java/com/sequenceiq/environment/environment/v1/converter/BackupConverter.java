package com.sequenceiq.environment.environment.v1.converter;

import com.sequenceiq.common.api.backup.request.BackupRequest;
import com.sequenceiq.common.api.backup.response.BackupResponse;
import com.sequenceiq.common.api.telemetry.model.CloudwatchParams;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.backup.model.BackupCloudwatchParams;
import com.sequenceiq.environment.environment.dto.EnvironmentBackup;
import com.sequenceiq.environment.environment.dto.telemetry.S3CloudStorageParameters;

@Component
public class BackupConverter {

    public BackupConverter() {
    }

    public EnvironmentBackup convert(BackupRequest request) {
        return createBackupFromRequest(request);
    }

    public EnvironmentBackup convert(TelemetryRequest request) {
        EnvironmentBackup backup = null;
        if (request != null) {
            return createBackupFromRequest(request.getLogging());
        }
        return backup;
    }

    public BackupResponse convert(EnvironmentBackup backup) {
        return createBackupResponseFromSource(backup);
    }

    public BackupRequest convertToRequest(EnvironmentBackup backup) {
        BackupRequest backupRequest = null;
        if (backup != null) {
            backupRequest = createBackupRequestFromEnvSource(backup);
        }
        return backupRequest;
    }

    private BackupResponse createBackupResponseFromSource(EnvironmentBackup backup) {
        BackupResponse backupResponse = null;
        if (backup != null) {
            backupResponse = new BackupResponse();
            backupResponse.setStorageLocation(backup.getStorageLocation());
            backupResponse.setS3(convertS3(backup.getS3()));
            backupResponse.setAdlsGen2(convertAdlsV2(backup.getAdlsGen2()));
            backupResponse.setGcs(convertGcs(backup.getGcs()));
            backupResponse.setCloudwatch(BackupCloudwatchParams.copy(backup.getCloudwatch()));
        }
        return backupResponse;
    }

    private BackupRequest createBackupRequestFromEnvSource(EnvironmentBackup backup) {
        BackupRequest backupRequest = null;
        if (backup != null) {
            backupRequest = new BackupRequest();
            backupRequest.setStorageLocation(backup.getStorageLocation());
            backupRequest.setS3(convertS3(backup.getS3()));
            backupRequest.setAdlsGen2(convertAdlsV2(backup.getAdlsGen2()));
            backupRequest.setGcs(convertGcs(backup.getGcs()));
            backupRequest.setCloudwatch(BackupCloudwatchParams.copy(backup.getCloudwatch()));
        }
        return backupRequest;
    }

    private EnvironmentBackup createBackupFromRequest(BackupRequest backupRequest) {
        EnvironmentBackup backup = null;
        if (backupRequest != null) {
            backup = new EnvironmentBackup();
            backup.setStorageLocation(backupRequest.getStorageLocation());
            backup.setS3(convertS3(backupRequest.getS3()));
            backup.setAdlsGen2(convertAdlsV2(backupRequest.getAdlsGen2()));
            backup.setGcs(convertGcs(backupRequest.getGcs()));
            backup.setCloudwatch(BackupCloudwatchParams.copy(backupRequest.getCloudwatch()));
        }
        return backup;
    }

    private EnvironmentBackup createBackupFromRequest(LoggingRequest loggingRequest) {
        EnvironmentBackup backup = null;
        if (loggingRequest != null) {
            backup = new EnvironmentBackup();
            backup.setStorageLocation(loggingRequest.getStorageLocation());
            backup.setS3(convertS3(loggingRequest.getS3()));
            backup.setAdlsGen2(convertAdlsV2(loggingRequest.getAdlsGen2()));
            backup.setGcs(convertGcs(loggingRequest.getGcs()));
            backup.setCloudwatch(convertBackupCloudwatchParams(loggingRequest.getCloudwatch()));
        }
        return backup;
    }

    private S3CloudStorageParameters convertS3(S3CloudStorageV1Parameters s3) {
        S3CloudStorageParameters s3CloudStorageParameters = null;
        if (s3 != null) {
            s3CloudStorageParameters = new S3CloudStorageParameters();
            s3CloudStorageParameters.setInstanceProfile(s3.getInstanceProfile());
            return s3CloudStorageParameters;
        }
        return s3CloudStorageParameters;
    }

    private S3CloudStorageV1Parameters convertS3(S3CloudStorageParameters s3) {
        S3CloudStorageV1Parameters s3CloudStorageV1Parameters = null;
        if (s3 != null) {
            s3CloudStorageV1Parameters = new S3CloudStorageV1Parameters();
            s3CloudStorageV1Parameters.setInstanceProfile(s3.getInstanceProfile());
            return s3CloudStorageV1Parameters;
        }
        return s3CloudStorageV1Parameters;
    }

    private AdlsGen2CloudStorageV1Parameters convertAdlsV2(AdlsGen2CloudStorageV1Parameters adlsV2) {
        AdlsGen2CloudStorageV1Parameters adlsGen2CloudStorageV1Parameters = null;
        if (adlsV2 != null) {
            adlsGen2CloudStorageV1Parameters = new AdlsGen2CloudStorageV1Parameters();
            adlsGen2CloudStorageV1Parameters.setAccountKey(adlsV2.getAccountKey());
            adlsGen2CloudStorageV1Parameters.setAccountName(adlsV2.getAccountName());
            adlsGen2CloudStorageV1Parameters.setManagedIdentity(adlsV2.getManagedIdentity());
            adlsGen2CloudStorageV1Parameters.setSecure(adlsV2.isSecure());
        }
        return adlsGen2CloudStorageV1Parameters;
    }

    private GcsCloudStorageV1Parameters convertGcs(GcsCloudStorageV1Parameters gcs) {
        GcsCloudStorageV1Parameters gcsCloudStorageV1Parameters = null;
        if (gcs != null) {
            gcsCloudStorageV1Parameters = new GcsCloudStorageV1Parameters();
            gcsCloudStorageV1Parameters.setServiceAccountEmail(gcs.getServiceAccountEmail());
        }
        return gcsCloudStorageV1Parameters;
    }

    private BackupCloudwatchParams convertBackupCloudwatchParams(CloudwatchParams cloudwatchParams) {
        BackupCloudwatchParams newCloudwatchParams = null;
        if (cloudwatchParams != null) {
            newCloudwatchParams = new BackupCloudwatchParams();
            newCloudwatchParams.setStreamKey(cloudwatchParams.getStreamKey());
            newCloudwatchParams.setInstanceProfile(cloudwatchParams.getInstanceProfile());
            newCloudwatchParams.setRegion(cloudwatchParams.getRegion());
        }
        return newCloudwatchParams;
    }
}