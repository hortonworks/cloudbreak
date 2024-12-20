package com.sequenceiq.cloudbreak.sdx.paas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;

import com.dyngr.core.AttemptResults;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.sdx.paas.flowpolling.FlowPollingService;
import com.sequenceiq.cloudbreak.sdx.paas.service.PaasSdxStartStopService;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@ExtendWith(MockitoExtension.class)
class PaasSdxStartStopServiceTest {
    private static final String SDX_CRN = "sdx-crn";

    private static final String POLL_TIMEOUT_MSG = "Datalake start/stop timed out";

    @Mock
    private SdxEndpoint sdxEndpoint;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @Mock
    private FlowPollingService flowPollingService;

    @InjectMocks
    private PaasSdxStartStopService paasSdxStartStopService;

    @BeforeEach
    void setup() {
        Field maxPollAttempt = ReflectionUtils.findField(PaasSdxStartStopService.class, "maxPollAttempt");
        ReflectionUtils.makeAccessible(maxPollAttempt);
        ReflectionUtils.setField(maxPollAttempt, paasSdxStartStopService, 1);

        Field sleepTime = ReflectionUtils.findField(PaasSdxStartStopService.class, "pollSleepTime");
        ReflectionUtils.makeAccessible(sleepTime);
        ReflectionUtils.setField(sleepTime, paasSdxStartStopService, 1);

        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
    }

    @Test
    void testStartSkippedIfAlreadyInProgress() {
        when(sdxEndpoint.getByCrn(SDX_CRN)).thenReturn(getSdxClusterResponse(SdxClusterStatusResponse.RUNNING));
        paasSdxStartStopService.startSdx(SDX_CRN);
        verify(sdxEndpoint).getByCrn(SDX_CRN);
        verifyNoMoreInteractions(sdxEndpoint);

        when(sdxEndpoint.getByCrn(SDX_CRN)).thenReturn(getSdxClusterResponse(SdxClusterStatusResponse.START_IN_PROGRESS));
        paasSdxStartStopService.startSdx(SDX_CRN);
        verify(sdxEndpoint, times(2)).getByCrn(SDX_CRN);
        verifyNoMoreInteractions(sdxEndpoint);
    }

    @Test
    void testStartSuccess() {
        when(sdxEndpoint.getByCrn(SDX_CRN)).thenReturn(getSdxClusterResponse(SdxClusterStatusResponse.STOPPED));
        when(flowPollingService.pollFlowIdAndReturnAttemptResult(any())).thenReturn(AttemptResults.finishWith(null));
        paasSdxStartStopService.startSdx(SDX_CRN);
        verify(sdxEndpoint).startByCrn(SDX_CRN);
    }

    @Test
    void testStartFailure() {
        when(sdxEndpoint.getByCrn(SDX_CRN)).thenReturn(getSdxClusterResponse(SdxClusterStatusResponse.STOPPED));
        when(flowPollingService.pollFlowIdAndReturnAttemptResult(any()))
                .thenReturn(AttemptResults.breakFor(new IllegalStateException("SDX flow operation failed")));
        assertThrows(UserBreakException.class, () -> paasSdxStartStopService.startSdx(SDX_CRN));
        verify(sdxEndpoint).startByCrn(SDX_CRN);
    }

    @Test
    void testStartTimeout() {
        when(sdxEndpoint.getByCrn(SDX_CRN)).thenReturn(getSdxClusterResponse(SdxClusterStatusResponse.STOPPED));
        when(flowPollingService.pollFlowIdAndReturnAttemptResult(any())).thenReturn(AttemptResults.justContinue());
        RuntimeException e = assertThrows(RuntimeException.class, () -> paasSdxStartStopService.startSdx(SDX_CRN));
        assertEquals(POLL_TIMEOUT_MSG, e.getMessage());
        verify(sdxEndpoint).startByCrn(SDX_CRN);
    }

    @Test
    void testStopSkippedIfAlreadyInProgress() {
        when(sdxEndpoint.getByCrn(SDX_CRN)).thenReturn(getSdxClusterResponse(SdxClusterStatusResponse.STOPPED));
        paasSdxStartStopService.stopSdx(SDX_CRN);
        verify(sdxEndpoint).getByCrn(SDX_CRN);
        verifyNoMoreInteractions(sdxEndpoint);

        when(sdxEndpoint.getByCrn(SDX_CRN)).thenReturn(getSdxClusterResponse(SdxClusterStatusResponse.STOP_IN_PROGRESS));
        paasSdxStartStopService.stopSdx(SDX_CRN);
        verify(sdxEndpoint, times(2)).getByCrn(SDX_CRN);
        verifyNoMoreInteractions(sdxEndpoint);
    }

    @Test
    void testStopSuccess() {
        when(sdxEndpoint.getByCrn(SDX_CRN)).thenReturn(getSdxClusterResponse(SdxClusterStatusResponse.RUNNING));
        when(flowPollingService.pollFlowIdAndReturnAttemptResult(any())).thenReturn(AttemptResults.finishWith(null));
        paasSdxStartStopService.stopSdx(SDX_CRN);
        verify(sdxEndpoint).stopByCrn(SDX_CRN);
    }

    @Test
    void testStopFailure() {
        when(sdxEndpoint.getByCrn(SDX_CRN)).thenReturn(getSdxClusterResponse(SdxClusterStatusResponse.RUNNING));
        when(flowPollingService.pollFlowIdAndReturnAttemptResult(any()))
                .thenReturn(AttemptResults.breakFor(new IllegalStateException("SDX flow operation failed")));
        assertThrows(UserBreakException.class, () -> paasSdxStartStopService.stopSdx(SDX_CRN));
        verify(sdxEndpoint).stopByCrn(SDX_CRN);
    }

    @Test
    void testStopTimeout() {
        when(sdxEndpoint.getByCrn(SDX_CRN)).thenReturn(getSdxClusterResponse(SdxClusterStatusResponse.RUNNING));
        when(flowPollingService.pollFlowIdAndReturnAttemptResult(any())).thenReturn(AttemptResults.justContinue());
        RuntimeException e = assertThrows(RuntimeException.class, () -> paasSdxStartStopService.stopSdx(SDX_CRN));
        assertEquals(POLL_TIMEOUT_MSG, e.getMessage());
        verify(sdxEndpoint).stopByCrn(SDX_CRN);
    }

    private SdxClusterResponse getSdxClusterResponse(SdxClusterStatusResponse status) {
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setStatus(status);
        return sdxClusterResponse;
    }
}
