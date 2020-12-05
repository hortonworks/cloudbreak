package com.sequenceiq.cloudbreak.telemetry.converter;

import java.nio.file.Paths;
import java.util.UUID;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2Config;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2ConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.GcsConfig;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.GcsConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.S3Config;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.S3ConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.support.SupportBundleConfiguration;
import com.sequenceiq.common.api.diagnostics.BaseDiagnosticsCollectionRequest;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.model.diagnostics.AwsDiagnosticParameters;
import com.sequenceiq.common.model.diagnostics.AwsDiagnosticParameters.AwsDiagnosticParametersBuilder;
import com.sequenceiq.common.model.diagnostics.AzureDiagnosticParameters;
import com.sequenceiq.common.model.diagnostics.AzureDiagnosticParameters.AzureDiagnosticParametersBuilder;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters.DiagnosticParametersBuilder;
import com.sequenceiq.common.model.diagnostics.GcsDiagnosticsParameters;
import com.sequenceiq.common.model.diagnostics.GcsDiagnosticsParameters.GcsDiagnosticParametersBuilder;

@Component
public class DiagnosticsDataToParameterConverter {

    private static final String DIAGNOSTICS_SUFFIX_PATH = "diagnostics";

    @Inject
    private SupportBundleConfiguration supportBundleConfiguration;

    @Inject
    private S3ConfigGenerator s3ConfigGenerator;

    @Inject
    private AdlsGen2ConfigGenerator adlsGen2ConfigGenerator;

    @Inject
    private GcsConfigGenerator gcsConfigGenerator;

    public DiagnosticParameters convert(BaseDiagnosticsCollectionRequest request, Telemetry telemetry,
            String clusterType, String clusterVersion, String accountId, String region) {
        Logging logging = telemetry.getLogging();
        DiagnosticParametersBuilder builder = DiagnosticParameters.builder();
        if (logging.getS3() != null) {
            AwsDiagnosticParametersBuilder awsBuilder = AwsDiagnosticParameters.builder();
            S3Config s3Config = s3ConfigGenerator.generateStorageConfig(logging.getStorageLocation());
            awsBuilder.withS3Bucket(s3Config.getBucket());
            awsBuilder.withS3Location(Paths.get(s3Config.getFolderPrefix(), DIAGNOSTICS_SUFFIX_PATH).toString());
            awsBuilder.withS3Region(region);
            builder.withCloudStorageDiagnosticsParameters(awsBuilder.build());
        } else if (logging.getAdlsGen2() != null) {
            AzureDiagnosticParametersBuilder azureBuilder = AzureDiagnosticParameters.builder();
            AdlsGen2Config adlsGen2Config = adlsGen2ConfigGenerator.generateStorageConfig(logging.getStorageLocation());
            azureBuilder.withAdlsv2StorageAccount(adlsGen2Config.getAccount());
            azureBuilder.withAdlsv2StorageContainer(adlsGen2Config.getFileSystem());
            azureBuilder.withAdlsv2StorageLocation(Paths.get(adlsGen2Config.getFolderPrefix(), DIAGNOSTICS_SUFFIX_PATH).toString());
            builder.withCloudStorageDiagnosticsParameters(azureBuilder.build());
        } else if (logging.getGcs() != null) {
            GcsDiagnosticParametersBuilder gcsBuilder = GcsDiagnosticsParameters.builder();
            GcsConfig gcsConfig = gcsConfigGenerator.generateStorageConfig(logging.getStorageLocation());
            gcsBuilder.withBucket(gcsConfig.getBucket());
            gcsBuilder.withGcsLocation(Paths.get(gcsConfig.getFolderPrefix(), DIAGNOSTICS_SUFFIX_PATH).toString());
            builder.withCloudStorageDiagnosticsParameters(gcsBuilder.build());
        }
        builder.withDestination(request.getDestination());
        builder.withDescription(request.getDescription());
        builder.withUuid(UUID.randomUUID().toString());
        builder.withClusterType(clusterType);
        builder.withClusterVersion(clusterVersion);
        builder.withAccountId(accountId);
        builder.withIssue(request.getIssue());
        builder.withLabels(request.getLabels());
        builder.withStartTime(request.getStartTime());
        builder.withEndTime(request.getEndTime());
        builder.withHostGroups(request.getHostGroups());
        builder.withHosts(request.getHosts());
        builder.withIncludeSaltLogs(request.getIncludeSaltLogs());
        builder.withUpdatePackage(request.getUpdatePackage());
        builder.withSkipValidation(request.getSkipValidation());
        builder.withAdditionalLogs(request.getAdditionalLogs());
        if (supportBundleConfiguration.isEnabled()) {
            builder.withDbusUrl(telemetry.getDatabusEndpoint());
            builder.withSupportBundleDbusStreamName(supportBundleConfiguration.getDbusStreamName());
            builder.withSupportBundleDbusAppName(supportBundleConfiguration.getDbusAppName());
        }
        return builder.build();
    }
}
