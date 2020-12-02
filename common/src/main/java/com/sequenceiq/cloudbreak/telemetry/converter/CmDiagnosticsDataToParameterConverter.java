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
import com.sequenceiq.common.api.diagnostics.BaseCmDiagnosticsCollectionRequest;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.model.diagnostics.CmDiagnosticsParameters;

@Component
public class CmDiagnosticsDataToParameterConverter {

    private static final String DIAGNOSTICS_SUFFIX_PATH = "diagnostics";

    @Inject
    private S3ConfigGenerator s3ConfigGenerator;

    @Inject
    private AdlsGen2ConfigGenerator adlsGen2ConfigGenerator;

    @Inject
    private GcsConfigGenerator gcsConfigGenerator;

    public CmDiagnosticsParameters convert(BaseCmDiagnosticsCollectionRequest request,
            Telemetry telemetry, String clusterName, String region) {
        Logging logging = telemetry.getLogging();
        CmDiagnosticsParameters.CmDiagnosticsParametersBuilder builder = CmDiagnosticsParameters.builder();
        if (logging.getS3() != null) {
            S3Config s3Config = s3ConfigGenerator.generateStorageConfig(logging.getStorageLocation());
            builder.withS3Bucket(s3Config.getBucket());
            builder.withS3Location(Paths.get(s3Config.getFolderPrefix(), DIAGNOSTICS_SUFFIX_PATH).toString());
            builder.withS3Region(region);
        } else if (logging.getAdlsGen2() != null) {
            AdlsGen2Config adlsGen2Config = adlsGen2ConfigGenerator.generateStorageConfig(logging.getStorageLocation());
            builder.withAdlsv2StorageAccount(adlsGen2Config.getAccount());
            builder.withAdlsv2StorageContainer(adlsGen2Config.getFileSystem());
            builder.withAdlsv2StorageLocation(Paths.get(adlsGen2Config.getFolderPrefix(), DIAGNOSTICS_SUFFIX_PATH).toString());
        } else if (logging.getGcs() != null) {
            GcsConfig gcsConfig = gcsConfigGenerator.generateStorageConfig(logging.getStorageLocation());
            builder.withGcsBucket(gcsConfig.getBucket());
            builder.withGcsLocation(Paths.get(gcsConfig.getFolderPrefix(), DIAGNOSTICS_SUFFIX_PATH).toString());
        }
        builder.withComments(request.getComments())
                .withDestination(request.getDestination())
                .withClusterName(clusterName)
                .withStartTime(request.getStartTime())
                .withEndTime(request.getEndTime())
                .withTicketNumber(request.getTicket())
                .withRoles(request.getRoles())
                .withBundleSizeBytes(request.getBundleSizeBytes())
                .withEnableMonitorMetricsCollection(request.getEnableMonitorMetricsCollection())
                .withUpdatePackage(request.getUpdatePackage())
                .withSkipValidation(request.getSkipValidation());
        return builder.build();
    }
}
