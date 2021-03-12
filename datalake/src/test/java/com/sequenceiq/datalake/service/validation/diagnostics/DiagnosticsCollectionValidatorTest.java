package com.sequenceiq.datalake.service.validation.diagnostics;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.telemetry.support.SupportBundleConfiguration;
import com.sequenceiq.common.api.diagnostics.BaseDiagnosticsCollectionRequest;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.api.telemetry.response.FeaturesResponse;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.FeatureSetting;

class DiagnosticsCollectionValidatorTest {

    private final DiagnosticsCollectionValidator underTest =
            new DiagnosticsCollectionValidator(
                    new SupportBundleConfiguration(false, null, null));

    @Test
    void testWithoutTelemetry() {
        BaseDiagnosticsCollectionRequest request = new BaseDiagnosticsCollectionRequest();
        StackV4Response stackV4Response = new StackV4Response();

        BadRequestException thrown = assertThrows(BadRequestException.class, () -> underTest.validate(request, stackV4Response));

        assertTrue(thrown.getMessage().contains("Telemetry is not enabled for Data Lake"));
    }

    @Test
    void testWithCloudStorageWithDisabledLogging() {
        BaseDiagnosticsCollectionRequest request = new BaseDiagnosticsCollectionRequest();
        request.setDestination(DiagnosticsDestination.CLOUD_STORAGE);
        StackV4Response stackV4Response = new StackV4Response();
        TelemetryResponse telemetry = new TelemetryResponse();
        stackV4Response.setTelemetry(telemetry);

        BadRequestException thrown = assertThrows(BadRequestException.class, () -> underTest.validate(request, stackV4Response));

        assertTrue(thrown.getMessage().contains("Cloud storage logging is disabled for Data Lake"));
    }

    @Test
    void testWithCloudStorageWithEmptyLogging() {
        BaseDiagnosticsCollectionRequest request = new BaseDiagnosticsCollectionRequest();
        request.setDestination(DiagnosticsDestination.CLOUD_STORAGE);
        StackV4Response stackV4Response = new StackV4Response();
        TelemetryResponse telemetry = new TelemetryResponse();
        LoggingResponse logging = new LoggingResponse();
        telemetry.setLogging(logging);
        stackV4Response.setTelemetry(telemetry);

        BadRequestException thrown = assertThrows(BadRequestException.class, () -> underTest.validate(request, stackV4Response));

        assertTrue(thrown.getMessage().contains("S3, ABFS or GCS cloud storage logging setting should be enabled for Data Lake"));
    }

    @Test
    void testWithEngDestinationAndDisabledLogCollection() {
        BaseDiagnosticsCollectionRequest request = new BaseDiagnosticsCollectionRequest();
        request.setDestination(DiagnosticsDestination.ENG);
        StackV4Response stackV4Response = new StackV4Response();
        TelemetryResponse telemetry = new TelemetryResponse();
        stackV4Response.setTelemetry(telemetry);

        BadRequestException thrown = assertThrows(BadRequestException.class, () -> underTest.validate(request, stackV4Response));

        assertTrue(thrown.getMessage().contains("Cluster log collection is not enabled for Data Lake"));
    }

    @Test
    void testWithValidEngDestination() {
        BaseDiagnosticsCollectionRequest request = new BaseDiagnosticsCollectionRequest();
        request.setDestination(DiagnosticsDestination.ENG);
        StackV4Response stackV4Response = new StackV4Response();
        TelemetryResponse telemetry = new TelemetryResponse();
        FeaturesResponse features = new FeaturesResponse();
        FeatureSetting featureSetting = new FeatureSetting();
        featureSetting.setEnabled(true);
        features.setClusterLogsCollection(featureSetting);
        telemetry.setFeatures(features);
        stackV4Response.setTelemetry(telemetry);

        underTest.validate(request, stackV4Response);
    }

    @Test
    void testWithSupportDestination() {
        BaseDiagnosticsCollectionRequest request = new BaseDiagnosticsCollectionRequest();
        request.setDestination(DiagnosticsDestination.SUPPORT);
        StackV4Response stackV4Response = new StackV4Response();
        TelemetryResponse telemetry = new TelemetryResponse();
        stackV4Response.setTelemetry(telemetry);

        BadRequestException thrown = assertThrows(BadRequestException.class, () -> underTest.validate(request, stackV4Response));

        assertTrue(thrown.getMessage().contains("Destination SUPPORT is not supported yet."));
    }
}