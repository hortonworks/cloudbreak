package com.sequenceiq.datalake.service.validation.diagnostics;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.telemetry.support.SupportBundleConfiguration;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.diagnostics.BaseCmDiagnosticsCollectionRequest;
import com.sequenceiq.common.api.diagnostics.BaseDiagnosticsCollectionRequest;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;

@Component
public class DiagnosticsCollectionValidator {

    private final SupportBundleConfiguration supportBundleConfiguration;

    public DiagnosticsCollectionValidator(SupportBundleConfiguration supportBundleConfiguration) {
        this.supportBundleConfiguration = supportBundleConfiguration;
    }

    public void validate(BaseCmDiagnosticsCollectionRequest request, StackV4Response stackV4Response) {
        validate(request.getDestination(), stackV4Response, true);
    }

    public void validate(BaseDiagnosticsCollectionRequest request, StackV4Response stackV4Response) {
        validate(request.getDestination(), stackV4Response, false);
    }

    public void validate(DiagnosticsDestination destination, StackV4Response stackV4Response, boolean cmBundle) {
        ValidationResult.ValidationResultBuilder validationBuilder = new ValidationResult.ValidationResultBuilder();
        TelemetryResponse telemetry = stackV4Response.getTelemetry();
        if (telemetry == null) {
            validationBuilder.error(String.format("Telemetry is not enabled for stack '%s'", stackV4Response.getName()));
        } else if (DiagnosticsDestination.CLOUD_STORAGE.equals(destination)) {
            validateCloudStorageSettings(stackV4Response, validationBuilder, telemetry);
        } else if (DiagnosticsDestination.ENG.equals(destination) && cmBundle) {
            validationBuilder.error("Cluster log collection with ENG destination is not supported for CM based diagnostics");
        } else if (DiagnosticsDestination.ENG.equals(destination) && isClusterLogCollectionDisabled(telemetry)) {
            validationBuilder.error(
                    String.format("Cluster log collection is not enabled for this stack '%s'", stackV4Response.getName()));
        } else if (DiagnosticsDestination.SUPPORT.equals(destination) && !isSupportBundleEnabled(cmBundle)) {
            validationBuilder.error(
                    String.format("Destination %s is not supported yet.", DiagnosticsDestination.SUPPORT.name()));
        }
        ValidationResult validationResult = validationBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    private boolean isSupportBundleEnabled(boolean cmBundle) {
        return cmBundle || supportBundleConfiguration.isEnabled();
    }

    private boolean isClusterLogCollectionDisabled(TelemetryResponse telemetry) {
        return !(telemetry.getFeatures() != null && telemetry.getFeatures().getClusterLogsCollection() != null
                && telemetry.getFeatures().getClusterLogsCollection().isEnabled());
    }

    private void validateCloudStorageSettings(StackV4Response stackV4Response,
            ValidationResult.ValidationResultBuilder validationBuilder, TelemetryResponse telemetry) {
        if (telemetry.getLogging() == null) {
            validationBuilder.error("Cloud storage logging is disabled for this cluster");
        } else if (telemetry.getLogging().getS3() == null
                && telemetry.getLogging().getAdlsGen2() == null
                && telemetry.getLogging().getGcs() == null) {
            validationBuilder.error(
                    String.format("S3, ABFS or GCS cloud storage logging setting should be enabled for stack '%s'.", stackV4Response.getName()));
        }
    }
}
