package com.sequenceiq.cloudbreak.controller.validation.diagnostics;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.telemetry.support.SupportBundleConfiguration;
import com.sequenceiq.cloudbreak.validation.AbstractCMDiagnosticsCollectionValidator;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.diagnostics.BaseCmDiagnosticsCollectionRequest;
import com.sequenceiq.common.api.diagnostics.BaseDiagnosticsCollectionRequest;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Component
public class DiagnosticsCollectionValidator extends AbstractCMDiagnosticsCollectionValidator<Telemetry> {

    public DiagnosticsCollectionValidator(SupportBundleConfiguration supportBundleConfiguration) {
        super(supportBundleConfiguration);
    }

    public void validate(BaseDiagnosticsCollectionRequest request, Stack stack, Telemetry telemetry) {
        validate(request.getDestination(), stack, telemetry, false);
    }

    public void validate(BaseCmDiagnosticsCollectionRequest request, Stack stack, Telemetry telemetry) {
        validate(request.getDestination(), stack, telemetry, true);
    }

    public void validate(DiagnosticsDestination destination, Stack stack, Telemetry telemetry, Boolean cmBundle) {
        validate(telemetry, destination, stack.getStatus(), stack.getName(), null, cmBundle);
    }

    @Override
    public boolean isSupportBundleEnabled(boolean cmBundle) {
        return cmBundle || getSupportBundleConfiguration().isEnabled();
    }

    @Override
    public boolean isClusterLogCollectionDisabled(Telemetry telemetry) {
        return !telemetry.isClusterLogsCollectionEnabled();
    }

    @Override
    public void validateCloudStorageSettings(Telemetry telemetry, String stackName,
            ValidationResult.ValidationResultBuilder validationBuilder) {
        if (telemetry.getLogging() == null) {
            validationBuilder.error(String.format("Cloud storage logging is disabled for Data Hub (name: '%s' )", stackName));
        } else if (telemetry.getLogging().getS3() == null
                && telemetry.getLogging().getAdlsGen2() == null
                && telemetry.getLogging().getGcs() == null) {
            validationBuilder.error(
                    String.format("S3, ABFS or GCS cloud storage logging setting should be enabled for Data Hub (name: '%s').", stackName));
        }
    }

    @Override
    public String getStackType() {
        return "Data Hub";
    }
}
