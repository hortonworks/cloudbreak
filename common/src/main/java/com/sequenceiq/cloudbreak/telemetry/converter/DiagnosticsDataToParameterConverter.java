package com.sequenceiq.cloudbreak.telemetry.converter;

import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.telemetry.DataBusEndpointProvider;
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

    @Inject
    private DataBusEndpointProvider dataBusEndpointProvider;

    public DiagnosticParameters convert(BaseDiagnosticsCollectionRequest request, Telemetry telemetry,
            String clusterType, String clusterVersion, String accountId, String region, String databusEndpoint) {
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
        builder.withClusterType(clusterType);
        builder.withClusterVersion(clusterVersion);
        builder.withAccountId(accountId);
        builder.withIssue(request.getIssue());
        builder.withLabels(request.getLabels());
        builder.withStartTime(request.getStartTime());
        builder.withEndTime(request.getEndTime());
        builder.withHostGroups(Optional.ofNullable(request.getHostGroups()).orElse(new HashSet<>()));
        builder.withHosts(Optional.ofNullable(request.getHosts()).orElse(new HashSet<>()));
        builder.withExcludeHosts(Optional.ofNullable(request.getExcludeHosts()).orElse(new HashSet<>()));
        builder.withIncludeSaltLogs(request.getIncludeSaltLogs());
        builder.withUpdatePackage(request.getUpdatePackage());
        builder.withSkipValidation(request.getSkipValidation());
        builder.withSkipWorkspaceCleanupOnStartup(request.getSkipWorkspaceCleanupOnStartup());
        builder.withSkipUnresponsiveHosts(request.getSkipUnresponsiveHosts());
        builder.withAdditionalLogs(request.getAdditionalLogs());
        if (supportBundleConfiguration.isEnabled()) {
            builder.withDbusUrl(databusEndpoint);
            builder.withDbusS3Url(dataBusEndpointProvider.getDatabusS3Endpoint(databusEndpoint));
            builder.withSupportBundleDbusStreamName(supportBundleConfiguration.getDbusStreamName());
            builder.withSupportBundleDbusAppName(supportBundleConfiguration.getDbusAppName());
        }
        return builder.build();
    }
}
