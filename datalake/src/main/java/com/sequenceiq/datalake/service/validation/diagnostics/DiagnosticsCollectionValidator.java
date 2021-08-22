package com.sequenceiq.datalake.service.validation.diagnostics;

import javax.ws.rs.ServiceUnavailableException;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.telemetry.support.SupportBundleConfiguration;
import com.sequenceiq.cloudbreak.validation.AbstractCMDiagnosticsCollectionValidator;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.diagnostics.BaseCmDiagnosticsCollectionRequest;
import com.sequenceiq.common.api.diagnostics.BaseDiagnosticsCollectionRequest;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;

@Component
public class DiagnosticsCollectionValidator extends AbstractCMDiagnosticsCollectionValidator<TelemetryResponse> {

    private final EntitlementService entitlementService;

    public DiagnosticsCollectionValidator(SupportBundleConfiguration supportBundleConfiguration, EntitlementService entitlementService) {
        super(supportBundleConfiguration);
        this.entitlementService = entitlementService;
    }

    public void validate(BaseCmDiagnosticsCollectionRequest request, StackV4Response stackV4Response) {
        validate(request.getDestination(), stackV4Response, true);
    }

    public void validate(BaseDiagnosticsCollectionRequest request, StackV4Response stackV4Response) {
        validate(request.getDestination(), stackV4Response, false);
    }

    public void validate(DiagnosticsDestination destination, StackV4Response stackV4Response, boolean cmBundle) {
        checkMaintenance(stackV4Response.getCrn());
        validate(stackV4Response.getTelemetry(), destination, stackV4Response.getStatus(), stackV4Response.getName(), null, cmBundle);
    }

    @Override
    public boolean isSupportBundleEnabled(boolean cmBundle) {
        return cmBundle || getSupportBundleConfiguration().isEnabled();
    }

    @Override
    public boolean isClusterLogCollectionDisabled(TelemetryResponse telemetry) {
        return !(telemetry.getFeatures() != null && telemetry.getFeatures().getClusterLogsCollection() != null
                && telemetry.getFeatures().getClusterLogsCollection().isEnabled());
    }

    @Override
    public void validateCloudStorageSettings(TelemetryResponse telemetry, String stackName,
            ValidationResult.ValidationResultBuilder validationBuilder) {
        if (telemetry.getLogging() == null) {
            validationBuilder.error(String.format("Cloud storage logging is disabled for Data Lake (name: '%s')", stackName));
        } else if (telemetry.getLogging().getS3() == null
                && telemetry.getLogging().getAdlsGen2() == null
                && telemetry.getLogging().getGcs() == null) {
            validationBuilder.error(
                    String.format("S3, ABFS or GCS cloud storage logging setting should be enabled for Data Lake (name: '%s')", stackName));
        }
    }

    @Override
    public String getStackType() {
        return "Data Lake";
    }

    private void checkMaintenance(String resourceCrn) {
        Crn crn = Crn.fromString(resourceCrn);
        if (crn != null && !entitlementService.isDiagnosticsEnabled(crn.getAccountId())) {
            throw new ServiceUnavailableException("DataLake diagnostics service is currently unavailable.");
        }
    }
}
