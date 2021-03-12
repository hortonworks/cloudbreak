package com.sequenceiq.freeipa.service.diagnostics;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.telemetry.support.SupportBundleConfiguration;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.diagnostics.BaseDiagnosticsCollectionRequest;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.api.telemetry.model.Features;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.type.FeatureSetting;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;

public class DiiagnosticsCollectionValidatorTest {

    private final DiagnosticsCollectionValidator underTest = new DiagnosticsCollectionValidator(
            new SupportBundleConfiguration(false, null, null));

    @Test
    void testValidateWithCloudStorageWithEmptyTelemetry() {
        BaseDiagnosticsCollectionRequest request = new BaseDiagnosticsCollectionRequest();
        request.setDestination(DiagnosticsDestination.CLOUD_STORAGE);
        Telemetry telemetry = new Telemetry();

        BadRequestException thrown = assertThrows(BadRequestException.class, () ->
                underTest.validate(request, createStackWithTelemetry(telemetry)));

        assertTrue(thrown.getMessage().contains("Cloud storage logging is disabled for FreeIPA"));
    }

    @Test
    void testValidateWithCloudStorageWithEmptyTelemetryLoggingSetting() {
        BaseDiagnosticsCollectionRequest request = new BaseDiagnosticsCollectionRequest();
        request.setDestination(DiagnosticsDestination.CLOUD_STORAGE);
        Telemetry telemetry = new Telemetry();
        telemetry.setLogging(new Logging());

        BadRequestException thrown = assertThrows(BadRequestException.class, () ->
                underTest.validate(request, createStackWithTelemetry(telemetry)));

        assertTrue(thrown.getMessage().contains("S3, ABFS or GCS cloud storage logging setting should be enabled for FreeIPA"));
    }

    @Test
    void testValidateWithValidCloudStorage() {
        BaseDiagnosticsCollectionRequest request = new BaseDiagnosticsCollectionRequest();
        request.setDestination(DiagnosticsDestination.CLOUD_STORAGE);
        Telemetry telemetry = new Telemetry();
        Logging logging = new Logging();
        logging.setS3(new S3CloudStorageV1Parameters());
        telemetry.setLogging(logging);

        underTest.validate(request, createStackWithTelemetry(telemetry));
    }

    @Test
    void testValidateWithSupportDestination() {
        BaseDiagnosticsCollectionRequest request = new BaseDiagnosticsCollectionRequest();
        request.setDestination(DiagnosticsDestination.SUPPORT);
        Telemetry telemetry = new Telemetry();

        BadRequestException thrown = assertThrows(BadRequestException.class, () ->
                underTest.validate(request, createStackWithTelemetry(telemetry)));

        assertTrue(thrown.getMessage().contains("Destination SUPPORT is not supported yet."));
    }

    @Test
    void testValidateWithInvalidEngDestination() {
        BaseDiagnosticsCollectionRequest request = new BaseDiagnosticsCollectionRequest();
        request.setDestination(DiagnosticsDestination.ENG);
        Telemetry telemetry = new Telemetry();

        BadRequestException thrown = assertThrows(BadRequestException.class, () ->
                underTest.validate(request, createStackWithTelemetry(telemetry)));

        assertTrue(thrown.getMessage().contains("Cluster log collection is not enabled for FreeIPA"));
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

        underTest.validate(request, createStackWithTelemetry(telemetry));
    }

    @Test
    void testValidateWithValidEngDestinationButWithWrongVersion() {
        BaseDiagnosticsCollectionRequest request = new BaseDiagnosticsCollectionRequest();
        request.setDestination(DiagnosticsDestination.ENG);
        Telemetry telemetry = new Telemetry();
        Features features = new Features();
        FeatureSetting clusterLogsCollection = new FeatureSetting();
        clusterLogsCollection.setEnabled(true);
        features.setClusterLogsCollection(clusterLogsCollection);
        telemetry.setFeatures(features);
        Stack stack = createStackWithTelemetry(telemetry);
        stack.setAppVersion("2.32.0-b48");

        BadRequestException thrown = assertThrows(BadRequestException.class, () ->
                underTest.validate(request, stack));
        assertTrue(thrown.getMessage().contains("Required FreeIPA min major/minor version is 2.33"));
    }

    private Stack createStackWithTelemetry(Telemetry telemetry) {
        Stack stack = new Stack();
        StackStatus status = new StackStatus();
        status.setStatus(Status.AVAILABLE);
        stack.setStackStatus(status);
        stack.setTelemetry(telemetry);
        stack.setAppVersion("2.35.0-b48");
        stack.setResourceCrn("stackCrn");
        stack.setName("stackCrn");
        return stack;
    }
}
