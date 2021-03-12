package com.sequenceiq.freeipa.service.diagnostics;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.telemetry.support.SupportBundleConfiguration;
import com.sequenceiq.cloudbreak.validation.AbstractDiagnosticsCollectionValidator;
import com.sequenceiq.cloudbreak.validation.MinAppVersionChecker;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.diagnostics.BaseDiagnosticsCollectionRequest;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.Stack;

@Component
public class DiagnosticsCollectionValidator extends AbstractDiagnosticsCollectionValidator<Telemetry, Status> {

    private static final int MIN_MAJOR_VERSION = 2;

    private static final int MIN_MINOR_VERSION = 33;

    public DiagnosticsCollectionValidator(SupportBundleConfiguration supportBundleConfiguration) {
        super(supportBundleConfiguration);
    }

    public void validate(BaseDiagnosticsCollectionRequest request, Stack stack) {
        validate(stack.getTelemetry(), request.getDestination(), stack.getStackStatus().getStatus(), stack.getName(), stack.getAppVersion());
    }

    @Override
    public void validateStackStatus(Status stackStatus, String stackName) {
        if (stackStatus.isFreeIpaUnreachableStatus()) {
            throw new BadRequestException(String.format("Cannot run diagnostics on FreeIPA (name: '%s') if the stack is in %s state",
                    stackName, stackStatus));
        }
    }

    @Override
    public MinAppVersionChecker getMinAppVersionChecker() {
        return new MinAppVersionChecker(MIN_MAJOR_VERSION, MIN_MINOR_VERSION);
    }

    @Override
    public void validateCloudStorageSettings(Telemetry telemetry, String stackName,
            ValidationResult.ValidationResultBuilder validationBuilder) {
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
    public boolean isClusterLogCollectionDisabled(Telemetry telemetry) {
        return !telemetry.isClusterLogsCollectionEnabled();
    }

    @Override
    public String getStackType() {
        return "FreeIPA";
    }

    @Override
    public boolean isSupportBundleEnabled(boolean cmBundle) {
        return getSupportBundleConfiguration().isEnabled();
    }
}
