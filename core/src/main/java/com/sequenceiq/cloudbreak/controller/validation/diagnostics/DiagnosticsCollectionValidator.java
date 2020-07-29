package com.sequenceiq.cloudbreak.controller.validation.diagnostics;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.diagnostics.BaseDiagnosticsCollectionRequest;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Component
public class DiagnosticsCollectionValidator {

    public void validate(BaseDiagnosticsCollectionRequest request, Telemetry telemetry, String stackCrn) {
        ValidationResult.ValidationResultBuilder validationBuilder = new ValidationResult.ValidationResultBuilder();
        if (telemetry == null) {
            validationBuilder.error(String.format("Telemetry is not enabled for stack '%s'", stackCrn));
        } else if (DiagnosticsDestination.CLOUD_STORAGE.equals(request.getDestination())) {
            validateCloudStorageSettings(telemetry, stackCrn, validationBuilder);
        } else if (DiagnosticsDestination.ENG.equals(request.getDestination()) &&
                telemetry.getFeatures() == null || !telemetry.getFeatures().getClusterLogsCollection().isEnabled()) {
            validationBuilder.error(
                    String.format("Cluster log collection is not enabled for this stack '%s'", stackCrn));
        } else if (DiagnosticsDestination.SUPPORT.equals(request.getDestination())) {
            validationBuilder.error(
                    String.format("Destination %s is not supported yet.", DiagnosticsDestination.SUPPORT.name()));
        }
        ValidationResult validationResult = validationBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    private void validateCloudStorageSettings(Telemetry telemetry, String stackCrn,
            ValidationResult.ValidationResultBuilder validationBuilder) {
        if (telemetry.getLogging() == null) {
            validationBuilder.error("Cloud storage logging is disabled for this cluster");
        } else if (telemetry.getLogging().getS3() == null
                && telemetry.getLogging().getAdlsGen2() == null) {
            validationBuilder.error(
                    String.format("S3 or ABFS cloud storage logging setting should be enabled for stack '%s'.", stackCrn));
        }
    }
}
