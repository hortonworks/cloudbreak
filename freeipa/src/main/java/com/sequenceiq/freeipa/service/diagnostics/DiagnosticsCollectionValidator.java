package com.sequenceiq.freeipa.service.diagnostics;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.telemetry.support.SupportBundleConfiguration;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.diagnostics.BaseDiagnosticsCollectionRequest;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Component
public class DiagnosticsCollectionValidator {

    private static final int MIN_MAJOR_VERSION = 2;

    private static final int MIN_MINOR_VERSION = 33;

    private final SupportBundleConfiguration supportBundleConfiguration;

    public DiagnosticsCollectionValidator(SupportBundleConfiguration supportBundleConfiguration) {
        this.supportBundleConfiguration = supportBundleConfiguration;
    }

    public void validate(BaseDiagnosticsCollectionRequest request, Telemetry telemetry, String stackCrn, String appVersion) {
        ValidationResult.ValidationResultBuilder validationBuilder = new ValidationResult.ValidationResultBuilder();
        DiagnosticsDestination destination = request.getDestination();
        if (!isAppVersionValid(appVersion)) {
            validationBuilder.error(String.format("Required freeipa min major/minor version is %d.%d for using diagnostics. " +
                            "Try it on newer environment.", MIN_MAJOR_VERSION, MIN_MINOR_VERSION));
        } else if (telemetry == null) {
            validationBuilder.error(String.format("Telemetry is not enabled for stack '%s'", stackCrn));
        } else if (DiagnosticsDestination.CLOUD_STORAGE.equals(destination)) {
            validateCloudStorageSettings(telemetry, stackCrn, validationBuilder);
        } else if (DiagnosticsDestination.ENG.equals(destination) && !telemetry.isClusterLogsCollectionEnabled()) {
            validationBuilder.error(
                    String.format("Cluster log collection is not enabled for this stack '%s'", stackCrn));
        } else if (DiagnosticsDestination.SUPPORT.equals(destination) && !supportBundleConfiguration.isEnabled()) {
            validationBuilder.error(
                    String.format("Destination %s is not supported yet.", DiagnosticsDestination.SUPPORT.name()));
        }
        ValidationResult validationResult = validationBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    private boolean isAppVersionValid(String appVersion) {
        boolean result = true;
        if (appVersion == null) {
            return result;
        }
        String withoutBuildNumber = appVersion.split("-")[0];
        String[] versionParts = withoutBuildNumber.split("\\.");
        if (versionParts.length > 1) {
            int majorVersion = Integer.parseInt(versionParts[0]);
            int minorVersion = Integer.parseInt(versionParts[1]);
            if (majorVersion < MIN_MAJOR_VERSION) {
                result = false;
            } else if (majorVersion == MIN_MAJOR_VERSION && minorVersion < MIN_MINOR_VERSION) {
                result = false;
            }
        }
        return result;
    }

    private void validateCloudStorageSettings(Telemetry telemetry, String stackCrn,
            ValidationResult.ValidationResultBuilder validationBuilder) {
        if (telemetry.getLogging() == null) {
            validationBuilder.error("Cloud storage logging is disabled for this cluster");
        } else if (telemetry.getLogging().getS3() == null
                && telemetry.getLogging().getAdlsGen2() == null
                && telemetry.getLogging().getGcs() == null) {
            validationBuilder.error(
                    String.format("S3, ABFS or GCS cloud storage logging setting should be enabled for stack '%s'.", stackCrn));
        }
    }
}
