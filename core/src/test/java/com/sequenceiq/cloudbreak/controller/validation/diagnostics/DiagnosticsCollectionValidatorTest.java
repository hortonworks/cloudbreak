package com.sequenceiq.cloudbreak.controller.validation.diagnostics;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.telemetry.support.SupportBundleConfiguration;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.diagnostics.BaseDiagnosticsCollectionRequest;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.api.telemetry.model.Features;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.type.FeatureSetting;

class DiagnosticsCollectionValidatorTest {

    private final DiagnosticsCollectionValidator underTest = new DiagnosticsCollectionValidator(
            new SupportBundleConfiguration(false, null, null));

    @Test
    void testValidateWithCloudStorageWithEmptyTelemetry() {
        BaseDiagnosticsCollectionRequest request = new BaseDiagnosticsCollectionRequest();
        request.setDestination(DiagnosticsDestination.CLOUD_STORAGE);
        Telemetry telemetry = new Telemetry();

        BadRequestException thrown = assertThrows(BadRequestException.class, () -> underTest.validate(request, createStack(), telemetry));

        assertTrue(thrown.getMessage().contains("Cloud storage logging is disabled for Data Hub"));
    }

    @Test
    void testValidateWithCloudStorageWithEmptyTelemetryLoggingSetting() {
        BaseDiagnosticsCollectionRequest request = new BaseDiagnosticsCollectionRequest();
        request.setDestination(DiagnosticsDestination.CLOUD_STORAGE);
        Telemetry telemetry = new Telemetry();
        telemetry.setLogging(new Logging());

        BadRequestException thrown = assertThrows(BadRequestException.class, () -> underTest.validate(request, createStack(), telemetry));

        assertTrue(thrown.getMessage().contains("S3, ABFS or GCS cloud storage logging setting should be enabled for Data Hub"));
    }

    @Test
    void testValidateWithValidCloudStorage() {
        BaseDiagnosticsCollectionRequest request = new BaseDiagnosticsCollectionRequest();
        request.setDestination(DiagnosticsDestination.CLOUD_STORAGE);
        Telemetry telemetry = new Telemetry();
        Logging logging = new Logging();
        logging.setS3(new S3CloudStorageV1Parameters());
        telemetry.setLogging(logging);

        underTest.validate(request, createStack(), telemetry);
    }

    @Test
    void testValidateWithSupportDestination() {
        BaseDiagnosticsCollectionRequest request = new BaseDiagnosticsCollectionRequest();
        request.setDestination(DiagnosticsDestination.SUPPORT);
        Telemetry telemetry = new Telemetry();

        BadRequestException thrown = assertThrows(BadRequestException.class, () -> underTest.validate(request, createStack(), telemetry));

        assertTrue(thrown.getMessage().contains("Destination SUPPORT is not supported yet."));
    }

    @Test
    void testValidateWithInvalidEngDestination() {
        BaseDiagnosticsCollectionRequest request = new BaseDiagnosticsCollectionRequest();
        request.setDestination(DiagnosticsDestination.ENG);
        Telemetry telemetry = new Telemetry();

        BadRequestException thrown = assertThrows(BadRequestException.class, () -> underTest.validate(request, createStack(), telemetry));

        assertTrue(thrown.getMessage().contains("Cluster log collection is not enabled for Data Hub"));
    }

    @Test
    void testValidateWithValidEngDestination() {
        BaseDiagnosticsCollectionRequest request = new BaseDiagnosticsCollectionRequest();
        request.setDestination(DiagnosticsDestination.ENG);
        Telemetry telemetry = new Telemetry();
        Features features = new Features();
        FeatureSetting clusterLogsCollection = new FeatureSetting();
        clusterLogsCollection.setEnabled(true);
        features.setClusterLogsCollection(clusterLogsCollection);
        telemetry.setFeatures(features);

        underTest.validate(request, createStack(), telemetry);
    }

    private Stack createStack() {
        Stack stack = new Stack();
        StackStatus status = new StackStatus();
        status.setStatus(Status.AVAILABLE);
        stack.setStackStatus(status);
        stack.setResourceCrn("stackCrn");
        stack.setName("stackName");
        return stack;
    }
}