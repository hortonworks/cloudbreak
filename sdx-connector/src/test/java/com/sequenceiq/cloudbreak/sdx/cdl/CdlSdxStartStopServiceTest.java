package com.sequenceiq.cloudbreak.sdx.cdl;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;

import com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.sdx.cdl.grpc.GrpcSdxCdlClient;
import com.sequenceiq.cloudbreak.sdx.cdl.service.CdlSdxStartStopService;

@ExtendWith(MockitoExtension.class)
class CdlSdxStartStopServiceTest {
    private static final String CDL_CRN = "crn:cdp:sdxsvc:us-west-1:test-account-id:instance:5a4d6c9b-e505-4f8a-9077-994fd05656e8";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private GrpcSdxCdlClient grpcSdxCdlClient;

    @InjectMocks
    private CdlSdxStartStopService cdlSdxStartStopService;

    @BeforeEach
    void setup() {
        Field maxPollAttempt = ReflectionUtils.findField(CdlSdxStartStopService.class, "maxPollAttempt");
        ReflectionUtils.makeAccessible(maxPollAttempt);
        ReflectionUtils.setField(maxPollAttempt, cdlSdxStartStopService, 1);

        Field sleepTime = ReflectionUtils.findField(CdlSdxStartStopService.class, "pollSleepTime");
        ReflectionUtils.makeAccessible(sleepTime);
        ReflectionUtils.setField(sleepTime, cdlSdxStartStopService, 1);
    }

    @Test
    void testNotEnabledNoInteraction() {
        setEnabled();
        when(entitlementService.isEntitledFor(any(), any())).thenReturn(Boolean.FALSE);

        cdlSdxStartStopService.startSdx(CDL_CRN);
        cdlSdxStartStopService.stopSdx(CDL_CRN);
        verifyNoInteractions(grpcSdxCdlClient);
    }

    @Test
    void testSuccessfulStart() {
        setEnabled();
        when(entitlementService.isEntitledFor(any(), any())).thenReturn(Boolean.TRUE);

        when(grpcSdxCdlClient.describeDatalake(CDL_CRN)).thenReturn(CdlCrudProto.DescribeDatalakeResponse.newBuilder()
                .setStatus(CdlCrudProto.StatusType.Value.RUNNING)
                .build());

        cdlSdxStartStopService.startSdx(CDL_CRN);
        verify(grpcSdxCdlClient).startDatalake(CDL_CRN);
    }

    @Test
    void testFailureStart() {
        setEnabled();
        when(entitlementService.isEntitledFor(any(), any())).thenReturn(Boolean.TRUE);

        when(grpcSdxCdlClient.describeDatalake(CDL_CRN)).thenReturn(CdlCrudProto.DescribeDatalakeResponse.newBuilder()
                .setStatus(CdlCrudProto.StatusType.Value.UNAVAILABLE)
                .build());

        assertThrows(UserBreakException.class, () -> cdlSdxStartStopService.startSdx(CDL_CRN));
        verify(grpcSdxCdlClient).startDatalake(CDL_CRN);
    }

    @Test
    void testTimeoutStart() {
        setEnabled();
        when(entitlementService.isEntitledFor(any(), any())).thenReturn(Boolean.TRUE);

        when(grpcSdxCdlClient.describeDatalake(CDL_CRN)).thenReturn(CdlCrudProto.DescribeDatalakeResponse.newBuilder()
                .setStatus(CdlCrudProto.StatusType.Value.PROVISIONING)
                .build());

        RuntimeException e = assertThrows(RuntimeException.class, () -> cdlSdxStartStopService.startSdx(CDL_CRN));
        assertEquals("Datalake start/stop timed out", e.getMessage());
        verify(grpcSdxCdlClient).startDatalake(CDL_CRN);
    }

    @Test
    void testSuccessfulStop() {
        setEnabled();
        when(entitlementService.isEntitledFor(any(), any())).thenReturn(Boolean.TRUE);

        when(grpcSdxCdlClient.describeDatalake(CDL_CRN)).thenReturn(CdlCrudProto.DescribeDatalakeResponse.newBuilder()
                .setStatus(CdlCrudProto.StatusType.Value.STOPPED)
                .build());

        cdlSdxStartStopService.stopSdx(CDL_CRN);
        verify(grpcSdxCdlClient).stopDatalake(CDL_CRN);
    }

    @Test
    void testFailureStop() {
        setEnabled();
        when(entitlementService.isEntitledFor(any(), any())).thenReturn(Boolean.TRUE);

        when(grpcSdxCdlClient.describeDatalake(CDL_CRN)).thenReturn(CdlCrudProto.DescribeDatalakeResponse.newBuilder()
                .setStatus(CdlCrudProto.StatusType.Value.UNAVAILABLE)
                .build());

        assertThrows(UserBreakException.class, () -> cdlSdxStartStopService.stopSdx(CDL_CRN));
        verify(grpcSdxCdlClient).stopDatalake(CDL_CRN);
    }

    @Test
    void testTimeoutStop() {
        setEnabled();
        when(entitlementService.isEntitledFor(any(), any())).thenReturn(Boolean.TRUE);

        when(grpcSdxCdlClient.describeDatalake(CDL_CRN)).thenReturn(CdlCrudProto.DescribeDatalakeResponse.newBuilder()
                .setStatus(CdlCrudProto.StatusType.Value.RUNNING)
                .build());

        RuntimeException e = assertThrows(RuntimeException.class, () -> cdlSdxStartStopService.stopSdx(CDL_CRN));
        assertEquals("Datalake start/stop timed out", e.getMessage());
        verify(grpcSdxCdlClient).stopDatalake(CDL_CRN);
    }

    private void setEnabled() {
        Field cdlEnabled = ReflectionUtils.findField(CdlSdxStartStopService.class, "cdlEnabled");
        ReflectionUtils.makeAccessible(cdlEnabled);
        ReflectionUtils.setField(cdlEnabled, cdlSdxStartStopService, true);
    }
}
