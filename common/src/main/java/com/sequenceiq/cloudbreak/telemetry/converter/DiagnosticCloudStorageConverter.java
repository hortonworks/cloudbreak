package com.sequenceiq.cloudbreak.telemetry.converter;

import java.nio.file.Paths;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2Config;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2ConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.GcsConfig;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.GcsConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.S3Config;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.S3ConfigGenerator;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.model.diagnostics.AwsDiagnosticParameters;
import com.sequenceiq.common.model.diagnostics.AzureDiagnosticParameters;
import com.sequenceiq.common.model.diagnostics.CloudStorageDiagnosticsParameters;
import com.sequenceiq.common.model.diagnostics.GcsDiagnosticsParameters;

@Component
public class DiagnosticCloudStorageConverter {

    private static final String DIAGNOSTICS_SUFFIX_PATH = "diagnostics";

    @Inject
    private S3ConfigGenerator s3ConfigGenerator;

    @Inject
    private AdlsGen2ConfigGenerator adlsGen2ConfigGenerator;

    @Inject
    private GcsConfigGenerator gcsConfigGenerator;

    public CloudStorageDiagnosticsParameters loggingResponseToCloudStorageDiagnosticsParameters(LoggingResponse logging, String region) {
        if (logging.getS3() != null) {
            return loggingResponseToS3(logging, region);
        } else if (logging.getAdlsGen2() != null) {
            return loggingResponseToAdlsGen2(logging);
        } else if (logging.getGcs() != null) {
            return loggingResponseToGcs(logging);
        } else {
            return null;
        }
    }

    public CloudStorageDiagnosticsParameters loggingToCloudStorageDiagnosticsParameters(Logging logging, String region) {
        if (logging.getS3() != null) {
            return loggingToS3(logging, region);
        } else if (logging.getAdlsGen2() != null) {
            return loggingToAdlsGen2(logging);
        } else if (logging.getGcs() != null) {
            return loggingToGcs(logging);
        } else {
            return null;
        }
    }

    public AwsDiagnosticParameters loggingToS3(Logging logging, String region) {
        AwsDiagnosticParameters.AwsDiagnosticParametersBuilder awsBuilder = AwsDiagnosticParameters.builder();
        S3Config s3Config = s3ConfigGenerator.generateStorageConfig(logging.getStorageLocation());
        return awsBuilder.withS3Bucket(s3Config.getBucket())
                .withS3Location(Paths.get(s3Config.getFolderPrefix(), DIAGNOSTICS_SUFFIX_PATH).toString())
                .withS3Region(region)
                .build();
    }

    public AzureDiagnosticParameters loggingToAdlsGen2(Logging logging) {
        AdlsGen2Config adlsGen2Config = adlsGen2ConfigGenerator.generateStorageConfig(logging.getStorageLocation());
        return AzureDiagnosticParameters.builder()
                .withAdlsv2StorageAccount(adlsGen2Config.getAccount())
                .withAdlsv2StorageContainer(adlsGen2Config.getFileSystem())
                .withAdlsv2StorageLocation(Paths.get(adlsGen2Config.getFolderPrefix(), DIAGNOSTICS_SUFFIX_PATH).toString())
                .build();
    }

    public GcsDiagnosticsParameters loggingToGcs(Logging logging) {
        GcsConfig gcsConfig = gcsConfigGenerator.generateStorageConfig(logging.getStorageLocation());
        return GcsDiagnosticsParameters.builder()
                .withBucket(gcsConfig.getBucket())
                .withGcsLocation(Paths.get(gcsConfig.getFolderPrefix(), DIAGNOSTICS_SUFFIX_PATH).toString())
                .build();
    }

    public AwsDiagnosticParameters loggingResponseToS3(LoggingResponse logging, String region) {
        AwsDiagnosticParameters.AwsDiagnosticParametersBuilder awsBuilder = AwsDiagnosticParameters.builder();
        S3Config s3Config = s3ConfigGenerator.generateStorageConfig(logging.getStorageLocation());
        return awsBuilder.withS3Bucket(s3Config.getBucket())
                .withS3Location(Paths.get(s3Config.getFolderPrefix(), DIAGNOSTICS_SUFFIX_PATH).toString())
                .withS3Region(region)
                .build();
    }

    public AzureDiagnosticParameters loggingResponseToAdlsGen2(LoggingResponse logging) {
        AdlsGen2Config adlsGen2Config = adlsGen2ConfigGenerator.generateStorageConfig(logging.getStorageLocation());
        return AzureDiagnosticParameters.builder()
                .withAdlsv2StorageAccount(adlsGen2Config.getAccount())
                .withAdlsv2StorageContainer(adlsGen2Config.getFileSystem())
                .withAdlsv2StorageLocation(Paths.get(adlsGen2Config.getFolderPrefix(), DIAGNOSTICS_SUFFIX_PATH).toString())
                .build();
    }

    public GcsDiagnosticsParameters loggingResponseToGcs(LoggingResponse logging) {
        GcsConfig gcsConfig = gcsConfigGenerator.generateStorageConfig(logging.getStorageLocation());
        return GcsDiagnosticsParameters.builder()
                .withBucket(gcsConfig.getBucket())
                .withGcsLocation(Paths.get(gcsConfig.getFolderPrefix(), DIAGNOSTICS_SUFFIX_PATH).toString())
                .build();
    }
}
