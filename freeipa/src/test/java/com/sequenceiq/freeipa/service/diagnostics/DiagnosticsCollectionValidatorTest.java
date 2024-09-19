package com.sequenceiq.freeipa.service.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.telemetry.support.SupportBundleConfiguration;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.diagnostics.BaseDiagnosticsCollectionRequest;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.api.telemetry.model.Features;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.service.image.ImageService;

@ExtendWith(MockitoExtension.class)
public class DiagnosticsCollectionValidatorTest {

    @InjectMocks
    private DiagnosticsCollectionValidator underTest;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ImageService imageService;

    @Mock
    private ImageEntity imageEntity;

    @BeforeEach
    public void setUp() {
        underTest = new DiagnosticsCollectionValidator(
                new SupportBundleConfiguration(false, null, null, false),
                entitlementService, imageService);
        when(imageService.getByStackId(any())).thenReturn(imageEntity);
        when(imageEntity.getDate()).thenReturn("2022-08-03");
    }

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
    void testValidateWithEngDestination() {
        BaseDiagnosticsCollectionRequest request = new BaseDiagnosticsCollectionRequest();
        request.setDestination(DiagnosticsDestination.ENG);
        Telemetry telemetry = new Telemetry();
        Features features = new Features();
        telemetry.setFeatures(features);

        BadRequestException thrown = assertThrows(BadRequestException.class, () -> underTest.validate(request, createStackWithTelemetry(telemetry)));
    }

    @Test
    void testValidateWithValidEngDestinationButWithWrongVersion() {
        BaseDiagnosticsCollectionRequest request = new BaseDiagnosticsCollectionRequest();
        request.setDestination(DiagnosticsDestination.ENG);
        Telemetry telemetry = new Telemetry();
        Features features = new Features();
        telemetry.setFeatures(features);
        Stack stack = createStackWithTelemetry(telemetry);
        when(imageEntity.getDate()).thenReturn("2020-01-01");

        BadRequestException thrown = assertThrows(BadRequestException.class, () ->
                underTest.validate(request, stack));
        assertEquals("Required FreeIPA min image date is 2021-01-28 for using diagnostics. Please upgrade your FreeIPA.", thrown.getMessage());
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
