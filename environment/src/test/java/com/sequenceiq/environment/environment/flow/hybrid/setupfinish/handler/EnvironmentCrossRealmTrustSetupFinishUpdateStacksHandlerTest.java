package com.sequenceiq.environment.environment.flow.hybrid.setupfinish.handler;

import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_SETUP_FINISH_FAILED;
import static com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishHandlerSelectors.SETUP_FINISH_TRUST_UPDATE_STACKS_HANDLER;
import static com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishStateSelectors.FINISH_TRUST_SETUP_FINISH_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateTrustedRealmRequest;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.MultipleFlowsResultEvaluator;
import com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishEvent;
import com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishFailedEvent;
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
class EnvironmentCrossRealmTrustSetupFinishUpdateStacksHandlerTest {

    private static final Long RESOURCE_ID = 123L;

    private static final String RESOURCE_CRN = "crn:env:freeipa:example";

    private static final String RESOURCE_NAME = "env-update-stacks-1";

    private static final String STACK_CRN_1 = "crn:cdp:datahub:us-west-1:acc:cluster:stack-1";

    private static final String STACK_CRN_2 = "crn:cdp:datahub:us-west-1:acc:cluster:stack-2";

    private static final String REALM = "EXAMPLE.COM";

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private DatahubPollerProvider datahubPollerProvider;

    @Mock
    private MultipleFlowsResultEvaluator multipleFlowsResultEvaluator;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private HandlerEvent<EnvironmentCrossRealmTrustSetupFinishEvent> handlerEvent;

    @Mock
    private Event<EnvironmentCrossRealmTrustSetupFinishEvent> event;

    @Mock
    private EnvironmentDto environmentDto;

    @InjectMocks
    private EnvironmentCrossRealmTrustSetupFinishUpdateStacksHandler handler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(handler, "attempt", 1);
        ReflectionTestUtils.setField(handler, "sleeptime", 1);
    }

    private EnvironmentCrossRealmTrustSetupFinishEvent createEventData() {
        return EnvironmentCrossRealmTrustSetupFinishEvent.builder()
                .withResourceId(RESOURCE_ID)
                .withResourceCrn(RESOURCE_CRN)
                .withResourceName(RESOURCE_NAME)
                .build();
    }

    private DescribeFreeIpaResponse describeFreeIpaResponseWithRealm(String realm) {
        TrustResponse trustResponse = new TrustResponse();
        trustResponse.setRealm(realm);
        DescribeFreeIpaResponse response = new DescribeFreeIpaResponse();
        response.setTrust(trustResponse);
        return response;
    }

    @Test
    void testSelectorReturnsCorrectValue() {
        assertEquals(SETUP_FINISH_TRUST_UPDATE_STACKS_HANDLER.selector(), handler.selector());
    }

    @Test
    void testDefaultFailureEventShouldReturnFailedEvent() {
        EnvironmentCrossRealmTrustSetupFinishEvent data = createEventData();
        Exception exception = new RuntimeException("failure");
        when(event.getData()).thenReturn(data);

        Selectable result = handler.defaultFailureEvent(RESOURCE_ID, exception, event);

        assertInstanceOf(EnvironmentCrossRealmTrustSetupFinishFailedEvent.class, result);
        EnvironmentCrossRealmTrustSetupFinishFailedEvent failed = (EnvironmentCrossRealmTrustSetupFinishFailedEvent) result;
        assertEquals(TRUST_SETUP_FINISH_FAILED, failed.getEnvironmentStatus());
        assertEquals(exception, failed.getException());
    }

    @Test
    void testDoAcceptPublicCloudShouldTriggerUpdateWithSaltUpdateRequired() {
        EnvironmentCrossRealmTrustSetupFinishEvent data = createEventData();
        when(handlerEvent.getData()).thenReturn(data);
        when(environmentService.findById(RESOURCE_ID)).thenReturn(Optional.of(environmentDto));
        when(environmentDto.getEnvironmentType()).thenReturn(EnvironmentType.PUBLIC_CLOUD);
        when(environmentDto.getResourceCrn()).thenReturn(RESOURCE_CRN);
        when(freeIpaService.describe(RESOURCE_CRN)).thenReturn(Optional.of(describeFreeIpaResponseWithRealm(REALM.toLowerCase())));
        when(clusterService.getStackCrnsForConfigUpdate(RESOURCE_CRN, EnvironmentType.PUBLIC_CLOUD)).thenReturn(List.of(STACK_CRN_1, STACK_CRN_2));

        FlowIdentifier flowId1 = new FlowIdentifier(FlowType.FLOW, "flow-1");
        FlowIdentifier flowId2 = new FlowIdentifier(FlowType.FLOW, "flow-2");
        when(stackV4Endpoint.triggerUpdateTrustedRealm(eq(0L), eq(STACK_CRN_1), any())).thenReturn(flowId1);
        when(stackV4Endpoint.triggerUpdateTrustedRealm(eq(0L), eq(STACK_CRN_2), any())).thenReturn(flowId2);

        List<FlowIdentifier> flowIdentifiers = List.of(flowId1, flowId2);
        when(datahubPollerProvider.multipleFlowsPoller(eq(RESOURCE_ID), eq(flowIdentifiers))).thenReturn(AttemptResults::justFinish);
        when(multipleFlowsResultEvaluator.collectFailed(flowIdentifiers)).thenReturn(List.of());

        Selectable result = handler.doAccept(handlerEvent);

        ArgumentCaptor<UpdateTrustedRealmRequest> requestCaptor = ArgumentCaptor.forClass(UpdateTrustedRealmRequest.class);
        verify(stackV4Endpoint).triggerUpdateTrustedRealm(eq(0L), eq(STACK_CRN_1), requestCaptor.capture());
        UpdateTrustedRealmRequest capturedRequest = requestCaptor.getValue();
        assertEquals(REALM, capturedRequest.getRealm());
        assertEquals(true, capturedRequest.isSaltUpdateRequired());

        assertInstanceOf(EnvironmentCrossRealmTrustSetupFinishEvent.class, result);
        assertEquals(FINISH_TRUST_SETUP_FINISH_EVENT.selector(), result.selector());
    }

    @Test
    void testDoAcceptHybridCloudShouldTriggerUpdateWithoutSaltUpdate() {
        EnvironmentCrossRealmTrustSetupFinishEvent data = createEventData();
        when(handlerEvent.getData()).thenReturn(data);
        when(environmentService.findById(RESOURCE_ID)).thenReturn(Optional.of(environmentDto));
        when(environmentDto.getEnvironmentType()).thenReturn(EnvironmentType.HYBRID);
        when(environmentDto.getResourceCrn()).thenReturn(RESOURCE_CRN);
        when(freeIpaService.describe(RESOURCE_CRN)).thenReturn(Optional.of(describeFreeIpaResponseWithRealm(REALM)));
        when(clusterService.getStackCrnsForConfigUpdate(RESOURCE_CRN, EnvironmentType.HYBRID)).thenReturn(List.of(STACK_CRN_1));

        FlowIdentifier flowId = new FlowIdentifier(FlowType.FLOW, "flow-3");
        when(stackV4Endpoint.triggerUpdateTrustedRealm(eq(0L), eq(STACK_CRN_1), any())).thenReturn(flowId);
        when(datahubPollerProvider.multipleFlowsPoller(eq(RESOURCE_ID), eq(List.of(flowId)))).thenReturn(AttemptResults::justFinish);
        when(multipleFlowsResultEvaluator.collectFailed(List.of(flowId))).thenReturn(List.of());

        Selectable result = handler.doAccept(handlerEvent);

        ArgumentCaptor<UpdateTrustedRealmRequest> requestCaptor = ArgumentCaptor.forClass(UpdateTrustedRealmRequest.class);
        verify(stackV4Endpoint).triggerUpdateTrustedRealm(eq(0L), eq(STACK_CRN_1), requestCaptor.capture());
        assertEquals(false, requestCaptor.getValue().isSaltUpdateRequired());

        assertInstanceOf(EnvironmentCrossRealmTrustSetupFinishEvent.class, result);
        assertEquals(FINISH_TRUST_SETUP_FINISH_EVENT.selector(), result.selector());
    }

    @Test
    void testDoAcceptWhenNoStacksShouldSkipPollingAndSucceed() {
        EnvironmentCrossRealmTrustSetupFinishEvent data = createEventData();
        when(handlerEvent.getData()).thenReturn(data);
        when(environmentService.findById(RESOURCE_ID)).thenReturn(Optional.of(environmentDto));
        when(environmentDto.getEnvironmentType()).thenReturn(EnvironmentType.PUBLIC_CLOUD);
        when(environmentDto.getResourceCrn()).thenReturn(RESOURCE_CRN);
        when(freeIpaService.describe(RESOURCE_CRN)).thenReturn(Optional.of(describeFreeIpaResponseWithRealm(REALM)));
        when(clusterService.getStackCrnsForConfigUpdate(RESOURCE_CRN, EnvironmentType.PUBLIC_CLOUD)).thenReturn(List.of());

        Selectable result = handler.doAccept(handlerEvent);

        verify(stackV4Endpoint, never()).triggerUpdateTrustedRealm(any(), any(), any());
        verify(datahubPollerProvider, never()).multipleFlowsPoller(any(), any());
        assertInstanceOf(EnvironmentCrossRealmTrustSetupFinishEvent.class, result);
        assertEquals(FINISH_TRUST_SETUP_FINISH_EVENT.selector(), result.selector());
    }

    @Test
    void testDoAcceptWhenEnvironmentNotFoundShouldUseEventCrnAndPublicCloudDefaults() {
        EnvironmentCrossRealmTrustSetupFinishEvent data = createEventData();
        when(handlerEvent.getData()).thenReturn(data);
        when(environmentService.findById(RESOURCE_ID)).thenReturn(Optional.empty());
        when(freeIpaService.describe(RESOURCE_CRN)).thenReturn(Optional.of(describeFreeIpaResponseWithRealm(REALM)));
        when(clusterService.getStackCrnsForConfigUpdate(RESOURCE_CRN, EnvironmentType.PUBLIC_CLOUD)).thenReturn(List.of(STACK_CRN_1));

        FlowIdentifier flowId = new FlowIdentifier(FlowType.FLOW, "flow-4");
        when(stackV4Endpoint.triggerUpdateTrustedRealm(eq(0L), eq(STACK_CRN_1), any())).thenReturn(flowId);
        when(datahubPollerProvider.multipleFlowsPoller(eq(RESOURCE_ID), eq(List.of(flowId)))).thenReturn(AttemptResults::justFinish);
        when(multipleFlowsResultEvaluator.collectFailed(List.of(flowId))).thenReturn(List.of());

        Selectable result = handler.doAccept(handlerEvent);

        ArgumentCaptor<UpdateTrustedRealmRequest> requestCaptor = ArgumentCaptor.forClass(UpdateTrustedRealmRequest.class);
        verify(stackV4Endpoint).triggerUpdateTrustedRealm(eq(0L), eq(STACK_CRN_1), requestCaptor.capture());
        // saltUpdateRequired=true because defaulted to PUBLIC_CLOUD
        assertEquals(true, requestCaptor.getValue().isSaltUpdateRequired());
        assertInstanceOf(EnvironmentCrossRealmTrustSetupFinishEvent.class, result);
    }

    @Test
    void testDoAcceptWhenFreeIpaDescribeReturnsEmptyShouldReturnFailedEvent() {
        EnvironmentCrossRealmTrustSetupFinishEvent data = createEventData();
        when(handlerEvent.getData()).thenReturn(data);
        when(environmentService.findById(RESOURCE_ID)).thenReturn(Optional.of(environmentDto));
        when(environmentDto.getEnvironmentType()).thenReturn(EnvironmentType.PUBLIC_CLOUD);
        when(environmentDto.getResourceCrn()).thenReturn(RESOURCE_CRN);
        when(freeIpaService.describe(RESOURCE_CRN)).thenReturn(Optional.empty());

        FlowIdentifier failedFlow = new FlowIdentifier(FlowType.FLOW, "flow-3");
        List<FlowIdentifier> flowIdentifiers = List.of(failedFlow);
        when(stackPollerService.updateSaltOnStacks(RESOURCE_ID, RESOURCE_CRN)).thenReturn(flowIdentifiers);
        when(datahubPollerProvider.multipleFlowsPoller(eq(RESOURCE_ID), eq(flowIdentifiers))).thenReturn(AttemptResults::justFinish);
        when(multipleFlowsResultEvaluator.collectFailed(flowIdentifiers)).thenReturn(List.of(failedFlow));

        Selectable result = handler.doAccept(handlerEvent);

        assertInstanceOf(EnvironmentCrossRealmTrustSetupFinishFailedEvent.class, result);
        EnvironmentCrossRealmTrustSetupFinishFailedEvent failed = (EnvironmentCrossRealmTrustSetupFinishFailedEvent) result;
        assertEquals(TRUST_SETUP_FINISH_FAILED, failed.getEnvironmentStatus());
        verify(stackV4Endpoint, never()).triggerUpdateTrustedRealm(any(), any(), any());
    }

    @Test
    void testDoAcceptWhenFailedFlowsShouldReturnFailedEvent() {
        EnvironmentCrossRealmTrustSetupFinishEvent data = createEventData();
        when(handlerEvent.getData()).thenReturn(data);
        when(environmentService.findById(RESOURCE_ID)).thenReturn(Optional.of(environmentDto));
        when(environmentDto.getEnvironmentType()).thenReturn(EnvironmentType.PUBLIC_CLOUD);
        when(environmentDto.getResourceCrn()).thenReturn(RESOURCE_CRN);
        when(freeIpaService.describe(RESOURCE_CRN)).thenReturn(Optional.of(describeFreeIpaResponseWithRealm(REALM)));
        when(clusterService.getStackCrnsForConfigUpdate(RESOURCE_CRN, EnvironmentType.PUBLIC_CLOUD)).thenReturn(List.of(STACK_CRN_1));

        FlowIdentifier failedFlow = new FlowIdentifier(FlowType.FLOW, "flow-5");
        when(stackV4Endpoint.triggerUpdateTrustedRealm(eq(0L), eq(STACK_CRN_1), any())).thenReturn(failedFlow);
        when(datahubPollerProvider.multipleFlowsPoller(eq(RESOURCE_ID), eq(List.of(failedFlow)))).thenReturn(AttemptResults::justFinish);
        when(multipleFlowsResultEvaluator.collectFailed(List.of(failedFlow))).thenReturn(List.of(failedFlow));

        Selectable result = handler.doAccept(handlerEvent);

        assertInstanceOf(EnvironmentCrossRealmTrustSetupFinishFailedEvent.class, result);
        assertEquals(TRUST_SETUP_FINISH_FAILED, ((EnvironmentCrossRealmTrustSetupFinishFailedEvent) result).getEnvironmentStatus());
    }

    @Test
    void testDoAcceptWhenPollerThrowsShouldReturnFailedEvent() {
        EnvironmentCrossRealmTrustSetupFinishEvent data = createEventData();
        when(handlerEvent.getData()).thenReturn(data);
        when(environmentService.findById(RESOURCE_ID)).thenReturn(Optional.of(environmentDto));
        when(environmentDto.getEnvironmentType()).thenReturn(EnvironmentType.PUBLIC_CLOUD);
        when(environmentDto.getResourceCrn()).thenReturn(RESOURCE_CRN);
        when(freeIpaService.describe(RESOURCE_CRN)).thenReturn(Optional.of(describeFreeIpaResponseWithRealm(REALM)));
        when(clusterService.getStackCrnsForConfigUpdate(RESOURCE_CRN, EnvironmentType.PUBLIC_CLOUD)).thenReturn(List.of(STACK_CRN_1));

        FlowIdentifier flowId = new FlowIdentifier(FlowType.FLOW, "flow-6");
        when(stackV4Endpoint.triggerUpdateTrustedRealm(eq(0L), eq(STACK_CRN_1), any())).thenReturn(flowId);
        when(datahubPollerProvider.multipleFlowsPoller(anyLong(), anyList()))
                .thenReturn(() -> {
                    throw new IllegalStateException("Poller error");
                });

        Selectable result = handler.doAccept(handlerEvent);

        assertInstanceOf(EnvironmentCrossRealmTrustSetupFinishFailedEvent.class, result);
        assertEquals(TRUST_SETUP_FINISH_FAILED, ((EnvironmentCrossRealmTrustSetupFinishFailedEvent) result).getEnvironmentStatus());
    }

    @Test
    void testDoAcceptWhenStackEndpointThrowsShouldReturnFailedEvent() {
        EnvironmentCrossRealmTrustSetupFinishEvent data = createEventData();
        when(handlerEvent.getData()).thenReturn(data);
        when(environmentService.findById(RESOURCE_ID)).thenReturn(Optional.of(environmentDto));
        when(environmentDto.getEnvironmentType()).thenReturn(EnvironmentType.PUBLIC_CLOUD);
        when(environmentDto.getResourceCrn()).thenReturn(RESOURCE_CRN);
        when(freeIpaService.describe(RESOURCE_CRN)).thenReturn(Optional.of(describeFreeIpaResponseWithRealm(REALM)));
        when(clusterService.getStackCrnsForConfigUpdate(RESOURCE_CRN, EnvironmentType.PUBLIC_CLOUD)).thenReturn(List.of(STACK_CRN_1));
        when(stackV4Endpoint.triggerUpdateTrustedRealm(eq(0L), eq(STACK_CRN_1), any()))
                .thenThrow(new RuntimeException("Stack endpoint error"));

        Selectable result = handler.doAccept(handlerEvent);

        assertInstanceOf(EnvironmentCrossRealmTrustSetupFinishFailedEvent.class, result);
        assertEquals(TRUST_SETUP_FINISH_FAILED, ((EnvironmentCrossRealmTrustSetupFinishFailedEvent) result).getEnvironmentStatus());
        verify(datahubPollerProvider, never()).multipleFlowsPoller(any(), any());
    }

    @Test
    void testDoAcceptRealmIsUpperCasedBeforeSending() {
        EnvironmentCrossRealmTrustSetupFinishEvent data = createEventData();
        when(handlerEvent.getData()).thenReturn(data);
        when(environmentService.findById(RESOURCE_ID)).thenReturn(Optional.of(environmentDto));
        when(environmentDto.getEnvironmentType()).thenReturn(EnvironmentType.PUBLIC_CLOUD);
        when(environmentDto.getResourceCrn()).thenReturn(RESOURCE_CRN);
        when(freeIpaService.describe(RESOURCE_CRN)).thenReturn(Optional.of(describeFreeIpaResponseWithRealm("example.com")));
        when(clusterService.getStackCrnsForConfigUpdate(RESOURCE_CRN, EnvironmentType.PUBLIC_CLOUD)).thenReturn(List.of(STACK_CRN_1));

        FlowIdentifier flowId = new FlowIdentifier(FlowType.FLOW, "flow-7");
        when(stackV4Endpoint.triggerUpdateTrustedRealm(eq(0L), eq(STACK_CRN_1), any())).thenReturn(flowId);
        when(datahubPollerProvider.multipleFlowsPoller(eq(RESOURCE_ID), eq(List.of(flowId)))).thenReturn(AttemptResults::justFinish);
        when(multipleFlowsResultEvaluator.collectFailed(List.of(flowId))).thenReturn(List.of());

        handler.doAccept(handlerEvent);

        ArgumentCaptor<UpdateTrustedRealmRequest> requestCaptor = ArgumentCaptor.forClass(UpdateTrustedRealmRequest.class);
        verify(stackV4Endpoint).triggerUpdateTrustedRealm(eq(0L), eq(STACK_CRN_1), requestCaptor.capture());
        assertEquals("EXAMPLE.COM", requestCaptor.getValue().getRealm());
    }

    @Test
    void testDoAcceptSuccessEventContainsCorrectResourceFields() {
        EnvironmentCrossRealmTrustSetupFinishEvent data = createEventData();
        when(handlerEvent.getData()).thenReturn(data);
        when(environmentService.findById(RESOURCE_ID)).thenReturn(Optional.of(environmentDto));
        when(environmentDto.getEnvironmentType()).thenReturn(EnvironmentType.PUBLIC_CLOUD);
        when(environmentDto.getResourceCrn()).thenReturn(RESOURCE_CRN);
        when(freeIpaService.describe(RESOURCE_CRN)).thenReturn(Optional.of(describeFreeIpaResponseWithRealm(REALM)));
        when(clusterService.getStackCrnsForConfigUpdate(RESOURCE_CRN, EnvironmentType.PUBLIC_CLOUD)).thenReturn(List.of(STACK_CRN_1));

        FlowIdentifier flowId = new FlowIdentifier(FlowType.FLOW, "flow-8");
        when(stackV4Endpoint.triggerUpdateTrustedRealm(eq(0L), eq(STACK_CRN_1), any())).thenReturn(flowId);
        when(datahubPollerProvider.multipleFlowsPoller(eq(RESOURCE_ID), eq(List.of(flowId)))).thenReturn(AttemptResults::justFinish);
        when(multipleFlowsResultEvaluator.collectFailed(List.of(flowId))).thenReturn(List.of());

        Selectable result = handler.doAccept(handlerEvent);

        assertInstanceOf(EnvironmentCrossRealmTrustSetupFinishEvent.class, result);
        EnvironmentCrossRealmTrustSetupFinishEvent successEvent = (EnvironmentCrossRealmTrustSetupFinishEvent) result;
        assertEquals(RESOURCE_CRN, successEvent.getResourceCrn());
        assertEquals(RESOURCE_ID, successEvent.getResourceId());
        assertEquals(RESOURCE_NAME, successEvent.getResourceName());
    }
}

