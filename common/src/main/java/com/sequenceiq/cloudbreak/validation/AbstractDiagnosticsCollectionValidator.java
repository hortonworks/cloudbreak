package com.sequenceiq.cloudbreak.validation;

import java.util.Optional;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.telemetry.support.SupportBundleConfiguration;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;

public abstract class AbstractDiagnosticsCollectionValidator<T, S> {

    private final SupportBundleConfiguration supportBundleConfiguration;

    public AbstractDiagnosticsCollectionValidator(SupportBundleConfiguration supportBundleConfiguration) {
        this.supportBundleConfiguration = supportBundleConfiguration;
    }

    public SupportBundleConfiguration getSupportBundleConfiguration() {
        return supportBundleConfiguration;
    }

    public void validate(T telemetry, DiagnosticsDestination destination, S stackStatus,
            String stackName, String version) {
        validate(telemetry, destination, stackStatus, stackName, version, false);
    }

    public void validate(T telemetry, DiagnosticsDestination destination, S stackStatus,
            String stackName, String version, boolean cmBundle) {
        ValidationResult.ValidationResultBuilder validationBuilder = new ValidationResult.ValidationResultBuilder();
        validateStackStatus(stackStatus, stackName);
        ImageDateChecker imageDateChecker = getImageDateChecker();
        if (!isImageDateAfter(version, imageDateChecker)) {
            validationBuilder.error(String.format("Required %s min image date is %s for using diagnostics. Please upgrade your %s.",
                    getStackType(), imageDateChecker.getMinImageDate(), getStackType()));
        } else if (telemetry == null) {
            validationBuilder.error(String.format("Telemetry is not enabled for %s (name: '%s')", getStackType(), stackName));
        } else if (DiagnosticsDestination.CLOUD_STORAGE.equals(destination)) {
            validateCloudStorageSettings(telemetry, stackName, validationBuilder);
        } else if (isEngDestinationWithCMBundle(destination, cmBundle)) {
            validationBuilder.error("Cluster log collection with ENG destination is not supported for CM based diagnostics");
        } else if (isEngDestinationDisabled(telemetry, destination)) {
            validationBuilder.error(
                    String.format("Cluster log collection is not enabled for %s (name: '%s')", getStackType(), stackName));
        } else if (isSupportDestinationDisabled(destination, cmBundle)) {
            validationBuilder.error(
                    String.format("Destination %s is not supported yet.", DiagnosticsDestination.SUPPORT.name()));
        }
        ValidationResult validationResult = validationBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    protected abstract void validateStackStatus(S stackStatus, String stackName);

    protected abstract boolean isSupportBundleEnabled(boolean cmBundle);

    protected abstract boolean isClusterLogCollectionDisabled(T telemetry);

    protected abstract void validateCloudStorageSettings(T telemetry, String stackName,
            ValidationResult.ValidationResultBuilder validationBuilder);

    protected abstract String getStackType();

    protected abstract ImageDateChecker getImageDateChecker();

    private boolean isAppVersionInvalid(String version, MinAppVersionChecker versionChecker) {
        return versionChecker != null && !versionChecker.isAppVersionValid(version);
    }

    private boolean isImageDateAfter(String imageDate, ImageDateChecker versionChecker) {
        return Optional.ofNullable(versionChecker)
                .map(checker -> checker.isImageDateValidOrNull(imageDate))
                .orElse(true);
    }

    private boolean isEngDestinationDisabled(T telemetry, DiagnosticsDestination destination) {
        return DiagnosticsDestination.ENG.equals(destination) && isClusterLogCollectionDisabled(telemetry);
    }

    private boolean isEngDestinationWithCMBundle(DiagnosticsDestination destination, boolean cmBundle) {
        return DiagnosticsDestination.ENG.equals(destination) && cmBundle;
    }

    private boolean isSupportDestinationDisabled(DiagnosticsDestination destination, boolean cmBundle) {
        return DiagnosticsDestination.SUPPORT.equals(destination) && !isSupportBundleEnabled(cmBundle);
    }
}
