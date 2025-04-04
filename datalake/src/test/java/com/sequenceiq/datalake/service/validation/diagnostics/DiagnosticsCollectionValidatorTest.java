package com.sequenceiq.datalake.service.validation.diagnostics;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.telemetry.support.SupportBundleConfiguration;
import com.sequenceiq.common.api.diagnostics.BaseDiagnosticsCollectionRequest;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.api.telemetry.response.FeaturesResponse;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;

@ExtendWith(MockitoExtension.class)
class DiagnosticsCollectionValidatorTest {

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:acc1:datalake:cluster1";

    @InjectMocks
    private DiagnosticsCollectionValidator underTest;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private SupportBundleConfiguration supportBundleConfiguration;

    @BeforeEach
    public void setUp() {
        underTest = new DiagnosticsCollectionValidator(supportBundleConfiguration, entitlementService);
        lenient().when(supportBundleConfiguration.isEnabled()).thenReturn(Boolean.FALSE);
        given(entitlementService.isDiagnosticsEnabled("acc1")).willReturn(true);
    }

    @Test
    void testWithoutTelemetry() {
        BaseDiagnosticsCollectionRequest request = new BaseDiagnosticsCollectionRequest();
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setCrn(DATALAKE_CRN);

        BadRequestException thrown = assertThrows(BadRequestException.class, () -> underTest.validate(request, stackV4Response));

        assertTrue(thrown.getMessage().contains("Telemetry is not enabled for Data Lake"));
    }

    @Test
    void testWithCloudStorageWithDisabledLogging() {
        BaseDiagnosticsCollectionRequest request = new BaseDiagnosticsCollectionRequest();
        request.setDestination(DiagnosticsDestination.CLOUD_STORAGE);
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setCrn(DATALAKE_CRN);
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
        stackV4Response.setCrn(DATALAKE_CRN);
        TelemetryResponse telemetry = new TelemetryResponse();
        LoggingResponse logging = new LoggingResponse();
        telemetry.setLogging(logging);
        stackV4Response.setTelemetry(telemetry);

        BadRequestException thrown = assertThrows(BadRequestException.class, () -> underTest.validate(request, stackV4Response));

        assertTrue(thrown.getMessage().contains("S3, ABFS or GCS cloud storage logging setting should be enabled for Data Lake"));
    }

    @Test
    void testWithEngDestination() {
        BaseDiagnosticsCollectionRequest request = new BaseDiagnosticsCollectionRequest();
        request.setDestination(DiagnosticsDestination.ENG);
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setCrn(DATALAKE_CRN);
        TelemetryResponse telemetry = new TelemetryResponse();
        FeaturesResponse features = new FeaturesResponse();
        telemetry.setFeatures(features);
        stackV4Response.setTelemetry(telemetry);

        assertThrows(BadRequestException.class, () -> underTest.validate(request, stackV4Response));
    }

    @Test
    void testWithSupportDestination() {
        BaseDiagnosticsCollectionRequest request = new BaseDiagnosticsCollectionRequest();
        request.setDestination(DiagnosticsDestination.SUPPORT);
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setCrn(DATALAKE_CRN);
        TelemetryResponse telemetry = new TelemetryResponse();
        stackV4Response.setTelemetry(telemetry);

        BadRequestException thrown = assertThrows(BadRequestException.class, () -> underTest.validate(request, stackV4Response));

        assertTrue(thrown.getMessage().contains("Destination SUPPORT is not supported yet."));
    }

    @Test
    void testValidateWithSupportDestinationWithoutCaseNumber() {
        when(supportBundleConfiguration.isEnabled()).thenReturn(Boolean.TRUE);
        BaseDiagnosticsCollectionRequest request = new BaseDiagnosticsCollectionRequest();
        request.setDestination(DiagnosticsDestination.SUPPORT);
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setCrn(DATALAKE_CRN);
        TelemetryResponse telemetry = new TelemetryResponse();
        stackV4Response.setTelemetry(telemetry);

        BadRequestException thrown = assertThrows(BadRequestException.class, () -> underTest.validate(request, stackV4Response));

        assertTrue(thrown.getMessage().contains("Case number is missing from the request, it is required for SUPPORT destination."));
    }

    @Test
    void testValidateWithSupportDestinationWithCaseNumber() {
        when(supportBundleConfiguration.isEnabled()).thenReturn(Boolean.TRUE);
        BaseDiagnosticsCollectionRequest request = new BaseDiagnosticsCollectionRequest();
        request.setDestination(DiagnosticsDestination.SUPPORT);
        request.setIssue("1234");
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setCrn(DATALAKE_CRN);
        TelemetryResponse telemetry = new TelemetryResponse();
        stackV4Response.setTelemetry(telemetry);

        underTest.validate(request, stackV4Response);
    }
}