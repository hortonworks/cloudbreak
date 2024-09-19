package com.sequenceiq.freeipa.service.diagnostics;

import jakarta.ws.rs.ServiceUnavailableException;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.telemetry.support.SupportBundleConfiguration;
import com.sequenceiq.cloudbreak.validation.AbstractDiagnosticsCollectionValidator;
import com.sequenceiq.cloudbreak.validation.ImageDateChecker;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.diagnostics.BaseDiagnosticsCollectionRequest;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.image.ImageService;

@Component
public class DiagnosticsCollectionValidator extends AbstractDiagnosticsCollectionValidator<Telemetry, Status> {

    private static final String MIN_IMAGE_DATE = "2021-01-28";

    private final EntitlementService entitlementService;

    private final ImageService imageService;

    public DiagnosticsCollectionValidator(SupportBundleConfiguration supportBundleConfiguration, EntitlementService entitlementService,
            ImageService imageService) {
        super(supportBundleConfiguration);
        this.entitlementService = entitlementService;
        this.imageService = imageService;
    }

    public void validate(BaseDiagnosticsCollectionRequest request, Stack stack) {
        checkMaintenance(stack.getResourceCrn());
        ImageEntity image = imageService.getByStackId(stack.getId());
        validate(stack.getTelemetry(), request.getDestination(), stack.getStackStatus().getStatus(), stack.getName(), image.getDate());
    }

    @Override
    public void validateStackStatus(Status stackStatus, String stackName) {
        if (stackStatus.isFreeIpaUnreachableStatus()) {
            throw new BadRequestException(String.format("Cannot run diagnostics on FreeIPA (name: '%s') if the stack is in %s state",
                    stackName, stackStatus));
        }
    }

    @Override
    public ImageDateChecker getImageDateChecker() {
        return new ImageDateChecker(MIN_IMAGE_DATE);
    }

    @Override
    public void validateCloudStorageSettings(Telemetry telemetry, String stackName,
            ValidationResultBuilder validationBuilder) {
        if (telemetry.getLogging() == null) {
            validationBuilder.error(String.format("Cloud storage logging is disabled for FreeIPA (name: '%s')", stackName));
        } else if (telemetry.getLogging().getS3() == null
                && telemetry.getLogging().getAdlsGen2() == null
                && telemetry.getLogging().getGcs() == null) {
            validationBuilder.error(
                    String.format("S3, ABFS or GCS cloud storage logging setting should be enabled for FreeIPA (name: '%s')", stackName));
        }
    }

    @Override
    public String getStackType() {
        return "FreeIPA";
    }

    @Override
    public boolean isSupportBundleEnabled(boolean cmBundle) {
        return getSupportBundleConfiguration().isEnabled();
    }

    private void checkMaintenance(String resourceCrn) {
        Crn crn = Crn.fromString(resourceCrn);
        if (crn != null && !entitlementService.isDiagnosticsEnabled(crn.getAccountId())) {
            throw new ServiceUnavailableException("FreeIPA diagnostics service is currently unavailable.");
        }
    }
}
