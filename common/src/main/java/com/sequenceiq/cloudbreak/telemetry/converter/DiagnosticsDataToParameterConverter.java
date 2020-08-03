package com.sequenceiq.cloudbreak.telemetry.converter;

import java.nio.file.Paths;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2Config;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2ConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.S3Config;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.S3ConfigGenerator;
import com.sequenceiq.common.api.diagnostics.BaseDiagnosticsCollectionRequest;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.model.diagnostics.AwsDiagnosticParameters;
import com.sequenceiq.common.model.diagnostics.AwsDiagnosticParameters.AwsDiagnosticParametersBuilder;
import com.sequenceiq.common.model.diagnostics.AzureDiagnosticParameters;
import com.sequenceiq.common.model.diagnostics.AzureDiagnosticParameters.AzureDiagnosticParametersBuilder;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters.DiagnosticParametersBuilder;

@Component
public class DiagnosticsDataToParameterConverter {

    private static final String DIAGNOSTICS_SUFFIX_PATH = "diagnostics";

    @Inject
    private S3ConfigGenerator s3ConfigGenerator;

    @Inject
    private AdlsGen2ConfigGenerator adlsGen2ConfigGenerator;

    public DiagnosticParameters convert(BaseDiagnosticsCollectionRequest request, Telemetry telemetry, String region) {
        Logging logging = telemetry.getLogging();
        DiagnosticParametersBuilder builder;
        if (logging.getS3() != null) {
            AwsDiagnosticParametersBuilder awsBuilder = AwsDiagnosticParameters.builder();
            builder = awsBuilder;
            S3Config s3Config = s3ConfigGenerator.generateStorageConfig(logging.getStorageLocation());
            awsBuilder.withS3Bucket(s3Config.getBucket());
            awsBuilder.withS3Location(Paths.get(s3Config.getFolderPrefix(), DIAGNOSTICS_SUFFIX_PATH).toString());
            awsBuilder.withS3Region(region);
        } else {
            AzureDiagnosticParametersBuilder azureBuilder = AzureDiagnosticParameters.builder();
            builder = azureBuilder;
            AdlsGen2Config adlsGen2Config = adlsGen2ConfigGenerator.generateStorageConfig(logging.getStorageLocation());
            azureBuilder.withAdlsv2StorageAccount(adlsGen2Config.getAccount());
            azureBuilder.withAdlsv2StorageContainer(adlsGen2Config.getFileSystem());
            azureBuilder.withAdlsv2StorageLocation(Paths.get(adlsGen2Config.getFolderPrefix(), DIAGNOSTICS_SUFFIX_PATH).toString());
        }
        builder.withDestination(request.getDestination());
        builder.withDescription(request.getDescription());
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
        return builder.build();
    }
}
