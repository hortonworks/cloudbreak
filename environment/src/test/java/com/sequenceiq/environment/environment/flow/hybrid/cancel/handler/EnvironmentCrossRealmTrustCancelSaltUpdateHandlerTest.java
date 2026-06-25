package com.sequenceiq.environment.environment.flow.hybrid.cancel.handler;

import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_CANCEL_SALT_UPDATE_FAILED;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelStateSelectors.FINISH_TRUST_CANCEL_CONFIG_REMOVAL_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.MultipleFlowsResultEvaluator;
import com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelEvent;
import com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelFailedEvent;
import com.sequenceiq.environment.environment.poller.DatahubPollerProvider;
import com.sequenceiq.environment.environment.service.sdx.SdxPollerService;
import com.sequenceiq.environment.environment.service.stack.StackPollerService;
import com.sequenceiq.environment.exception.DatahubOperationFailedException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class EnvironmentCrossRealmTrustCancelSaltUpdateHandlerTest {

    private static final long RESOURCE_ID = 1L;

    private static final String RESOURCE_CRN = "crn:cdp:environments:us-west-1:1234:environment:abc";

    private static final String RESOURCE_NAME = "test-env";

    @Mock
    private StackPollerService stackPollerService;

    @Mock
    private DatahubPollerProvider datahubPollerProvider;

    @Mock
    private MultipleFlowsResultEvaluator multipleFlowsResultEvaluator;

    @Mock
    private SdxPollerService sdxPollerService;

    @InjectMocks
    private EnvironmentCrossRealmTrustCancelSaltUpdateHandler underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "attempt", 1);
        ReflectionTestUtils.setField(underTest, "sleeptime", 1);
    }

    @Test
    void testSelectorReturnsCorrectValue() {
        assertThat(underTest.selector()).isEqualTo("TRUST_CANCEL_SALT_UPDATE_HANDLER");
    }

    @Test
    void testDoAcceptSuccessfulSaltUpdateOnDatalakesAndDatahubs() {
        List<FlowIdentifier> datahubFlows = List.of(new FlowIdentifier(FlowType.FLOW, "flow-1"));
        when(stackPollerService.updateSaltOnDatahubStacks(RESOURCE_ID, RESOURCE_CRN)).thenReturn(datahubFlows);
        when(datahubPollerProvider.multipleFlowsPoller(eq(RESOURCE_ID), eq(datahubFlows))).thenReturn(AttemptResults::justFinish);
        when(multipleFlowsResultEvaluator.collectFailed(datahubFlows)).thenReturn(List.of());

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(buildEvent())));

        verify(sdxPollerService).updateSaltOnAttachedDatalakeClusters(RESOURCE_ID, RESOURCE_NAME);
        verify(stackPollerService).updateSaltOnDatahubStacks(RESOURCE_ID, RESOURCE_CRN);
        assertThat(result).isInstanceOf(EnvironmentCrossRealmTrustCancelEvent.class);
        assertThat(result.selector()).isEqualTo(FINISH_TRUST_CANCEL_CONFIG_REMOVAL_EVENT.selector());
    }

    @Test
    void testDoAcceptWhenNoDatahubFlowsSkipsPolling() {
        when(stackPollerService.updateSaltOnDatahubStacks(RESOURCE_ID, RESOURCE_CRN)).thenReturn(null);

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(buildEvent())));

        verify(sdxPollerService).updateSaltOnAttachedDatalakeClusters(RESOURCE_ID, RESOURCE_NAME);
        assertThat(result).isInstanceOf(EnvironmentCrossRealmTrustCancelEvent.class);
        assertThat(result.selector()).isEqualTo(FINISH_TRUST_CANCEL_CONFIG_REMOVAL_EVENT.selector());
    }

    @Test
    void testDoAcceptWhenDatalakeSaltUpdateFailsReturnsFailedEvent() {
        doThrow(new RuntimeException("datalake salt update failed"))
                .when(sdxPollerService).updateSaltOnAttachedDatalakeClusters(RESOURCE_ID, RESOURCE_NAME);

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(buildEvent())));

        assertThat(result).isInstanceOf(EnvironmentCrossRealmTrustCancelFailedEvent.class);
        EnvironmentCrossRealmTrustCancelFailedEvent failedEvent = (EnvironmentCrossRealmTrustCancelFailedEvent) result;
        assertThat(failedEvent.getEnvironmentStatus()).isEqualTo(TRUST_CANCEL_SALT_UPDATE_FAILED);
    }

    @Test
    void testDoAcceptWhenDatahubFlowsFailReturnsFailedEvent() {
        FlowIdentifier failedFlow = new FlowIdentifier(FlowType.FLOW, "failed-flow");
        List<FlowIdentifier> datahubFlows = List.of(failedFlow);
        when(stackPollerService.updateSaltOnDatahubStacks(RESOURCE_ID, RESOURCE_CRN)).thenReturn(datahubFlows);
        when(datahubPollerProvider.multipleFlowsPoller(eq(RESOURCE_ID), eq(datahubFlows))).thenReturn(AttemptResults::justFinish);
        when(multipleFlowsResultEvaluator.collectFailed(datahubFlows)).thenReturn(List.of(failedFlow));

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(buildEvent())));

        assertThat(result).isInstanceOf(EnvironmentCrossRealmTrustCancelFailedEvent.class);
        EnvironmentCrossRealmTrustCancelFailedEvent failedEvent = (EnvironmentCrossRealmTrustCancelFailedEvent) result;
        assertThat(failedEvent.getEnvironmentStatus()).isEqualTo(TRUST_CANCEL_SALT_UPDATE_FAILED);
        assertThat(failedEvent.getException()).isInstanceOf(DatahubOperationFailedException.class);
    }

    @Test
    void testDoAcceptWhenDatahubPollerTimesOutReturnsFailedEvent() {
        List<FlowIdentifier> datahubFlows = List.of(new FlowIdentifier(FlowType.FLOW, "flow-1"));
        when(stackPollerService.updateSaltOnDatahubStacks(RESOURCE_ID, RESOURCE_CRN)).thenReturn(datahubFlows);
        when(datahubPollerProvider.multipleFlowsPoller(eq(RESOURCE_ID), eq(datahubFlows)))
                .thenReturn(AttemptResults::justContinue);

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(buildEvent())));

        assertThat(result).isInstanceOf(EnvironmentCrossRealmTrustCancelFailedEvent.class);
        EnvironmentCrossRealmTrustCancelFailedEvent failedEvent = (EnvironmentCrossRealmTrustCancelFailedEvent) result;
        assertThat(failedEvent.getEnvironmentStatus()).isEqualTo(TRUST_CANCEL_SALT_UPDATE_FAILED);
        assertThat(failedEvent.getException()).isInstanceOf(DatahubOperationFailedException.class);
    }

    @Test
    void testDefaultFailureEventReturnsCorrectEvent() {
        Exception ex = new RuntimeException("unexpected");

        Selectable result = underTest.defaultFailureEvent(RESOURCE_ID, ex, new Event<>(buildEvent()));

        assertThat(result).isInstanceOf(EnvironmentCrossRealmTrustCancelFailedEvent.class);
        EnvironmentCrossRealmTrustCancelFailedEvent failedEvent = (EnvironmentCrossRealmTrustCancelFailedEvent) result;
        assertThat(failedEvent.getEnvironmentStatus()).isEqualTo(TRUST_CANCEL_SALT_UPDATE_FAILED);
        assertThat(failedEvent.getException()).isSameAs(ex);
    }

    private EnvironmentCrossRealmTrustCancelEvent buildEvent() {
        return EnvironmentCrossRealmTrustCancelEvent.builder()
                .withResourceId(RESOURCE_ID)
                .withResourceCrn(RESOURCE_CRN)
                .withResourceName(RESOURCE_NAME)
                .build();
    }
}
