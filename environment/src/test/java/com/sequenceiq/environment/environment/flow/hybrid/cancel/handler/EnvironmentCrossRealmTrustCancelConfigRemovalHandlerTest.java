package com.sequenceiq.environment.environment.flow.hybrid.cancel.handler;

import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_CANCEL_CONFIG_REMOVAL_FAILED;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelStateSelectors.TRUST_CANCEL_TRUST_ENTITY_DELETE_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.MultipleFlowsResultEvaluator;
import com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelEvent;
import com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelFailedEvent;
import com.sequenceiq.environment.environment.poller.DatahubPollerProvider;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.cluster.ClusterService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustResponse;

@ExtendWith(MockitoExtension.class)
class EnvironmentCrossRealmTrustCancelConfigRemovalHandlerTest {

    private static final long RESOURCE_ID = 1L;

    private static final String RESOURCE_CRN = "crn:cdp:environments:us-west-1:1234:environment:abc";

    private static final String RESOURCE_NAME = "test-env";

    private static final String REALM = "EXAMPLE.COM";

    @Mock
    private DatahubPollerProvider datahubPollerProvider;

    @Mock
    private MultipleFlowsResultEvaluator multipleFlowsResultEvaluator;

    @Mock
    private ClusterService clusterService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private EnvironmentDto environmentDto;

    @InjectMocks
    private EnvironmentCrossRealmTrustCancelConfigRemovalHandler underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "attempt", 1);
        ReflectionTestUtils.setField(underTest, "sleeptime", 1);
    }

    @Test
    void testSelectorReturnsCorrectValue() {
        assertThat(underTest.selector()).isEqualTo("TRUST_CANCEL_CONFIG_REMOVAL_HANDLER");
    }

    @Test
    void testDoAcceptRemovesCmConfigAndReturnsEntityDeleteEvent() {
        when(environmentService.findById(RESOURCE_ID)).thenReturn(Optional.of(environmentDto));
        List<FlowIdentifier> removeFlows = List.of(new FlowIdentifier(FlowType.FLOW, "remove-flow-1"));
        when(clusterService.removeTrustedRealmConfigFromClusters(eq(Optional.of(environmentDto)), eq(REALM))).thenReturn(removeFlows);
        when(datahubPollerProvider.multipleFlowsPoller(eq(RESOURCE_ID), eq(removeFlows))).thenReturn(AttemptResults::justFinish);
        when(multipleFlowsResultEvaluator.collectFailed(removeFlows)).thenReturn(List.of());

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(buildEvent(REALM))));

        verify(clusterService).removeTrustedRealmConfigFromClusters(eq(Optional.of(environmentDto)), eq(REALM));
        assertThat(result).isInstanceOf(EnvironmentCrossRealmTrustCancelEvent.class);
        assertThat(result.selector()).isEqualTo(TRUST_CANCEL_TRUST_ENTITY_DELETE_EVENT.selector());
    }

    @Test
    void testDoAcceptWhenRealmNotOnEventFetchesRealmFromFreeIpa() {
        when(environmentService.findById(RESOURCE_ID)).thenReturn(Optional.of(environmentDto));
        mockFreeIpaDescribe(REALM);
        List<FlowIdentifier> removeFlows = List.of(new FlowIdentifier(FlowType.FLOW, "remove-flow-1"));
        when(clusterService.removeTrustedRealmConfigFromClusters(eq(Optional.of(environmentDto)), eq(REALM))).thenReturn(removeFlows);
        when(datahubPollerProvider.multipleFlowsPoller(eq(RESOURCE_ID), eq(removeFlows))).thenReturn(AttemptResults::justFinish);
        when(multipleFlowsResultEvaluator.collectFailed(removeFlows)).thenReturn(List.of());

        // No realm on the event — handler must call FreeIPA
        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(buildEvent(null))));

        verify(freeIpaService).describe(RESOURCE_CRN);
        verify(clusterService).removeTrustedRealmConfigFromClusters(eq(Optional.of(environmentDto)), eq(REALM));
        assertThat(result.selector()).isEqualTo(TRUST_CANCEL_TRUST_ENTITY_DELETE_EVENT.selector());
    }

    @Test
    void testDoAcceptWhenRealmNotOnEventAndFreeIpaHasNoTrustShouldReturnFailedEvent() {
        when(environmentService.findById(RESOURCE_ID)).thenReturn(Optional.of(environmentDto));
        when(freeIpaService.describe(RESOURCE_CRN)).thenReturn(Optional.empty());

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(buildEvent(null))));

        assertThat(result).isInstanceOf(EnvironmentCrossRealmTrustCancelFailedEvent.class);
        assertThat(((EnvironmentCrossRealmTrustCancelFailedEvent) result).getEnvironmentStatus()).isEqualTo(TRUST_CANCEL_CONFIG_REMOVAL_FAILED);
        verify(clusterService, never()).removeTrustedRealmConfigFromClusters(any(), any());
    }

    @Test
    void testDoAcceptWhenEnvironmentNotFoundReturnsSuccessWithEmptyFlows() {
        when(environmentService.findById(RESOURCE_ID)).thenReturn(Optional.empty());

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(buildEvent(REALM))));

        assertThat(result.selector()).isEqualTo(TRUST_CANCEL_TRUST_ENTITY_DELETE_EVENT.selector());
        verify(clusterService, never()).removeTrustedRealmConfigFromClusters(any(), any());
    }

    @Test
    void testDoAcceptWhenClusterServiceThrowsShouldReturnFailedEvent() {
        when(environmentService.findById(RESOURCE_ID)).thenReturn(Optional.of(environmentDto));
        doThrow(new CloudbreakServiceException("CM not reachable"))
                .when(clusterService).removeTrustedRealmConfigFromClusters(any(), any());

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(buildEvent(REALM))));

        assertThat(result).isInstanceOf(EnvironmentCrossRealmTrustCancelFailedEvent.class);
        EnvironmentCrossRealmTrustCancelFailedEvent failedEvent = (EnvironmentCrossRealmTrustCancelFailedEvent) result;
        assertThat(failedEvent.getEnvironmentStatus()).isEqualTo(TRUST_CANCEL_CONFIG_REMOVAL_FAILED);
        assertThat(failedEvent.getException()).isInstanceOf(CloudbreakServiceException.class);
    }

    @Test
    void testDoAcceptWhenRemoveFlowsHaveFailuresShouldReturnFailedEvent() {
        when(environmentService.findById(RESOURCE_ID)).thenReturn(Optional.of(environmentDto));
        FlowIdentifier failedRemoveFlow = new FlowIdentifier(FlowType.FLOW, "remove-flow-failed");
        List<FlowIdentifier> removeFlows = List.of(failedRemoveFlow);
        when(clusterService.removeTrustedRealmConfigFromClusters(eq(Optional.of(environmentDto)), eq(REALM))).thenReturn(removeFlows);
        when(datahubPollerProvider.multipleFlowsPoller(eq(RESOURCE_ID), eq(removeFlows))).thenReturn(AttemptResults::justFinish);
        when(multipleFlowsResultEvaluator.collectFailed(removeFlows)).thenReturn(List.of(failedRemoveFlow));

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(buildEvent(REALM))));

        assertThat(result).isInstanceOf(EnvironmentCrossRealmTrustCancelFailedEvent.class);
        assertThat(((EnvironmentCrossRealmTrustCancelFailedEvent) result).getEnvironmentStatus()).isEqualTo(TRUST_CANCEL_CONFIG_REMOVAL_FAILED);
    }

    @Test
    void testDoAcceptWhenRemoveFlowPollerTimesOutShouldReturnFailedEvent() {
        when(environmentService.findById(RESOURCE_ID)).thenReturn(Optional.of(environmentDto));
        List<FlowIdentifier> removeFlows = List.of(new FlowIdentifier(FlowType.FLOW, "remove-flow-1"));
        when(clusterService.removeTrustedRealmConfigFromClusters(eq(Optional.of(environmentDto)), eq(REALM))).thenReturn(removeFlows);
        when(datahubPollerProvider.multipleFlowsPoller(eq(RESOURCE_ID), eq(removeFlows))).thenReturn(() -> {
            throw new IllegalStateException("Timeout");
        });

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(buildEvent(REALM))));

        assertThat(result).isInstanceOf(EnvironmentCrossRealmTrustCancelFailedEvent.class);
        assertThat(((EnvironmentCrossRealmTrustCancelFailedEvent) result).getEnvironmentStatus()).isEqualTo(TRUST_CANCEL_CONFIG_REMOVAL_FAILED);
    }

    @Test
    void testDoAcceptWhenNoRemoveFlowsSkipsPolling() {
        when(environmentService.findById(RESOURCE_ID)).thenReturn(Optional.of(environmentDto));
        when(clusterService.removeTrustedRealmConfigFromClusters(eq(Optional.of(environmentDto)), eq(REALM))).thenReturn(List.of());

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(buildEvent(REALM))));

        assertThat(result).isInstanceOf(EnvironmentCrossRealmTrustCancelEvent.class);
        assertThat(result.selector()).isEqualTo(TRUST_CANCEL_TRUST_ENTITY_DELETE_EVENT.selector());
        verify(datahubPollerProvider, never()).multipleFlowsPoller(anyLong(), anyList());
    }

    @Test
    void testDoAcceptOutboundEventCarriesRealm() {
        when(environmentService.findById(RESOURCE_ID)).thenReturn(Optional.of(environmentDto));
        when(clusterService.removeTrustedRealmConfigFromClusters(eq(Optional.of(environmentDto)), eq(REALM))).thenReturn(List.of());

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(buildEvent(REALM))));

        assertThat(result).isInstanceOf(EnvironmentCrossRealmTrustCancelEvent.class);
        assertThat(((EnvironmentCrossRealmTrustCancelEvent) result).getRealm()).isEqualTo(REALM);
    }

    @Test
    void testDefaultFailureEventReturnsFailedEvent() {
        Exception ex = new RuntimeException("unexpected failure");

        Selectable result = underTest.defaultFailureEvent(RESOURCE_ID, ex, new Event<>(buildEvent(REALM)));

        assertThat(result).isInstanceOf(EnvironmentCrossRealmTrustCancelFailedEvent.class);
        EnvironmentCrossRealmTrustCancelFailedEvent failedEvent = (EnvironmentCrossRealmTrustCancelFailedEvent) result;
        assertThat(failedEvent.getEnvironmentStatus()).isEqualTo(TRUST_CANCEL_CONFIG_REMOVAL_FAILED);
        assertThat(failedEvent.getException()).isSameAs(ex);
    }

    private void mockFreeIpaDescribe(String realm) {
        TrustResponse trustResponse = new TrustResponse();
        trustResponse.setRealm(realm);
        DescribeFreeIpaResponse response = new DescribeFreeIpaResponse();
        response.setTrust(trustResponse);
        when(freeIpaService.describe(RESOURCE_CRN)).thenReturn(Optional.of(response));
    }

    private EnvironmentCrossRealmTrustCancelEvent buildEvent(String realm) {
        return EnvironmentCrossRealmTrustCancelEvent.builder()
                .withResourceId(RESOURCE_ID)
                .withResourceCrn(RESOURCE_CRN)
                .withResourceName(RESOURCE_NAME)
                .withRealm(realm)
                .build();
    }
}

