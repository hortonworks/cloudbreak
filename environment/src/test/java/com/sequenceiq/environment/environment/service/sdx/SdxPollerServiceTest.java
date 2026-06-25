package com.sequenceiq.environment.environment.service.sdx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.core.AttemptResults;
import com.sequenceiq.environment.environment.flow.DatalakeMultipleFlowsResultEvaluator;
import com.sequenceiq.environment.environment.poller.SdxPollerProvider;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@ExtendWith(MockitoExtension.class)
public class SdxPollerServiceTest {

    private static final String ENV_NAME = "envName";

    private static final Long ENV_ID = 1L;

    @InjectMocks
    private SdxPollerService underTest;

    @Mock
    private SdxService sdxService;

    @Mock
    private SdxPollerProvider sdxPollerProvider;

    @Mock
    private DatalakeMultipleFlowsResultEvaluator multipleFlowsResultEvaluator;

    @Mock
    private Function<String, FlowIdentifier> consumer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "attempt", 1);
        ReflectionTestUtils.setField(underTest, "sleeptime", 1);
    }

    @Test
    public void testGetExecuteSdxOperationsAndGetCrnsWhenShouldSkipTheConsumer() {

        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setStatus(SdxClusterStatusResponse.STOPPED);
        when(sdxService.list(ENV_NAME)).thenReturn(List.of(sdxClusterResponse));

        List<FlowIdentifier> actual = underTest.getExecuteSdxOperationsAndGetCrns(ENV_NAME, consumer, Set.of(SdxClusterStatusResponse.STOPPED));

        verify(consumer, never()).apply(any());
        assertEquals(1, actual.size());
    }

    @Test
    public void testGetExecuteSdxOperationsAndGetCrnsWhenShouldNotSkipTheConsumer() {
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setStatus(SdxClusterStatusResponse.RUNNING);
        sdxClusterResponse.setCrn("crn");
        when(sdxService.list(ENV_NAME)).thenReturn(List.of(sdxClusterResponse));

        List<FlowIdentifier> actual = underTest.getExecuteSdxOperationsAndGetCrns(ENV_NAME, consumer, Set.of(SdxClusterStatusResponse.STOPPED));

        verify(consumer).apply(any());
        assertEquals(1, actual.size());
    }

    @Test
    public void testGetExecuteSdxOperationsAndGetCrnsWhenOneSkipedAndOneNot() {

        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setStatus(SdxClusterStatusResponse.RUNNING);
        sdxClusterResponse.setCrn("crn");
        SdxClusterResponse sdxClusterResponse1 = new SdxClusterResponse();
        sdxClusterResponse1.setStatus(SdxClusterStatusResponse.STOPPED);
        sdxClusterResponse1.setCrn("crn1");
        when(sdxService.list(ENV_NAME)).thenReturn(List.of(sdxClusterResponse, sdxClusterResponse1));

        List<FlowIdentifier> actual = underTest.getExecuteSdxOperationsAndGetCrns(ENV_NAME, consumer, Set.of(SdxClusterStatusResponse.STOPPED));

        verify(consumer).apply("crn");
        verify(consumer, never()).apply("crn1");
        assertEquals(2, actual.size());
    }

    @Test
    public void testUpdateSaltOnAttachedDatalakeClustersCallsSdxService() {
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setStatus(SdxClusterStatusResponse.RUNNING);
        sdxClusterResponse.setCrn("sdx-crn");
        FlowIdentifier flowId = new FlowIdentifier(FlowType.FLOW, "flow-1");
        when(sdxService.list(ENV_NAME)).thenReturn(List.of(sdxClusterResponse));
        when(sdxService.updateSalt("sdx-crn")).thenReturn(flowId);
        when(sdxPollerProvider.flowListPoller(anyLong(), any())).thenReturn(AttemptResults::justFinish);

        underTest.updateSaltOnAttachedDatalakeClusters(ENV_ID, ENV_NAME);

        verify(sdxService).updateSalt("sdx-crn");
    }

    @Test
    public void testUpdateSaltOnAttachedDatalakeClustersSkipsWhenAlreadyInProgress() {
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setStatus(SdxClusterStatusResponse.SALT_UPDATE_IN_PROGRESS);
        sdxClusterResponse.setCrn("sdx-crn");
        when(sdxService.list(ENV_NAME)).thenReturn(List.of(sdxClusterResponse));
        when(sdxPollerProvider.flowListPoller(anyLong(), any())).thenReturn(AttemptResults::justFinish);

        underTest.updateSaltOnAttachedDatalakeClusters(ENV_ID, ENV_NAME);

        verify(sdxService, never()).updateSalt(any());
    }

}

