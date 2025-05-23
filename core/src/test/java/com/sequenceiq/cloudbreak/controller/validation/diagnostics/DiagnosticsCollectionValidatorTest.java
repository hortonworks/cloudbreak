package com.sequenceiq.cloudbreak.controller.validation.diagnostics;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
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

@ExtendWith(MockitoExtension.class)
class DiagnosticsCollectionValidatorTest {

    @InjectMocks
    private DiagnosticsCollectionValidator underTest;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private SupportBundleConfiguration supportBundleConfiguration;

    public void setUp(boolean supportBundleEnabled) {
        underTest = new DiagnosticsCollectionValidator(supportBundleConfiguration, entitlementService);
        lenient().when(supportBundleConfiguration.isEnabled()).thenReturn(Boolean.FALSE);
    }

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
    void testValidateWithSupportDestinationWithoutCaseNumber() {
        when(supportBundleConfiguration.isEnabled()).thenReturn(Boolean.TRUE);
        BaseDiagnosticsCollectionRequest request = new BaseDiagnosticsCollectionRequest();
        request.setDestination(DiagnosticsDestination.SUPPORT);
        Telemetry telemetry = new Telemetry();

        BadRequestException thrown = assertThrows(BadRequestException.class, () -> underTest.validate(request, createStack(), telemetry));

        assertTrue(thrown.getMessage().contains("Case number is missing from the request, it is required for SUPPORT destination."));
    }

    @Test
    void testValidateWithSupportDestinationWithCaseNumber() {
        when(supportBundleConfiguration.isEnabled()).thenReturn(Boolean.TRUE);
        BaseDiagnosticsCollectionRequest request = new BaseDiagnosticsCollectionRequest();
        request.setDestination(DiagnosticsDestination.SUPPORT);
        request.setIssue("1234");
        Telemetry telemetry = new Telemetry();

        underTest.validate(request, createStack(), telemetry);
    }

    @Test
    void testValidateWithEngDestination() {
        BaseDiagnosticsCollectionRequest request = new BaseDiagnosticsCollectionRequest();
        request.setDestination(DiagnosticsDestination.ENG);
        Telemetry telemetry = new Telemetry();
        Features features = new Features();
        telemetry.setFeatures(features);

        assertThrows(BadRequestException.class, () -> underTest.validate(request, createStack(), telemetry));
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