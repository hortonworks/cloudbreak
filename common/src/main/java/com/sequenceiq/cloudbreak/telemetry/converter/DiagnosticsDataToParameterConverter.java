package com.sequenceiq.cloudbreak.telemetry.converter;

import java.util.HashSet;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.telemetry.DataBusEndpointProvider;
import com.sequenceiq.cloudbreak.telemetry.support.SupportBundleConfiguration;
import com.sequenceiq.common.api.diagnostics.BaseDiagnosticsCollectionRequest;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters.DiagnosticParametersBuilder;

@Component
public class DiagnosticsDataToParameterConverter {

    @Inject
    private SupportBundleConfiguration supportBundleConfiguration;

    @Inject
    private DiagnosticCloudStorageConverter diagnosticCloudStorageConverter;

    @Inject
    private DataBusEndpointProvider dataBusEndpointProvider;

    public DiagnosticParameters convert(BaseDiagnosticsCollectionRequest request, Telemetry telemetry,
            String clusterType, String clusterVersion, String accountId, String region, String databusEndpoint) {
        Logging logging = telemetry.getLogging();
        DiagnosticParametersBuilder builder = DiagnosticParameters.builder()
                .withCloudStorageDiagnosticsParameters(diagnosticCloudStorageConverter.loggingToCloudStorageDiagnosticsParameters(logging, region))
                .withDestination(request.getDestination())
                .withDescription(request.getDescription())
                .withClusterType(clusterType)
                .withClusterVersion(clusterVersion)
                .withAccountId(accountId)
                .withIssue(request.getIssue())
                .withLabels(request.getLabels())
                .withStartTime(request.getStartTime())
                .withEndTime(request.getEndTime())
                .withHostGroups(Optional.ofNullable(request.getHostGroups()).orElse(new HashSet<>()))
                .withHosts(Optional.ofNullable(request.getHosts()).orElse(new HashSet<>()))
                .withExcludeHosts(Optional.ofNullable(request.getExcludeHosts()).orElse(new HashSet<>()))
                .withIncludeSaltLogs(request.getIncludeSaltLogs())
                .withIncludeSarOutput(request.getIncludeSarOutput())
                .withIncludeNginxReport(request.getIncludeNginxReport())
                .withIncludeSeLinuxReport(request.getIncludeSeLinuxReport())
                .withUpdatePackage(request.getUpdatePackage())
                .withSkipValidation(request.getSkipValidation())
                .withSkipWorkspaceCleanupOnStartup(request.getSkipWorkspaceCleanupOnStartup())
                .withSkipUnresponsiveHosts(request.getSkipUnresponsiveHosts())
                .withAdditionalLogs(request.getAdditionalLogs());
        if (supportBundleConfiguration.isEnabled()) {
            builder.withDbusUrl(databusEndpoint)
                    .withDbusS3Url(dataBusEndpointProvider.getDatabusS3Endpoint(databusEndpoint, region))
                    .withSupportBundleDbusStreamName(supportBundleConfiguration.getDbusStreamName())
                    .withSupportBundleDbusAppName(supportBundleConfiguration.getDbusAppName());
        }
        return builder.build();
    }
}
