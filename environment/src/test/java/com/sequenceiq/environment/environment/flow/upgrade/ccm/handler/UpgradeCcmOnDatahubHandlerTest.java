package com.sequenceiq.environment.environment.flow.upgrade.ccm.handler;

import static com.sequenceiq.environment.environment.EnvironmentStatus.UPGRADE_CCM_ON_DATAHUB_FAILED;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmHandlerSelectors.UPGRADE_CCM_DATAHUB_HANDLER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.model.CcmUpgradeResponseType;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXCcmUpgradeV1Response;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmEvent;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmFailedEvent;
import com.sequenceiq.environment.environment.service.datahub.DatahubService;
import com.sequenceiq.environment.environment.service.datahub.DatahubUpgradeCcmPollerService;
import com.sequenceiq.environment.exception.DatahubOperationFailedException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
class UpgradeCcmOnDatahubHandlerTest {

    private static final String TEST_ACCOUNT_ID = "someAccountId";

    private static final String TEST_ENV_CRN = "someEnvCrn";

    private static final String TEST_ENV_NAME = "someEnvName";

    private static final String TEST_STACK1_CRN = "someStackCrn1";

    private static final String TEST_STACK2_CRN = "someStackCrn2";

    private static final long TEST_ENV_ID = 123L;

    @Mock
    private EventSender mockEventSender;

    @Mock
    private Event.Headers mockEventHeaders;

    @Mock
    private EnvironmentDto mockEnvironmentDto;

    @Mock
    private Event<EnvironmentDto> mockEnvironmentDtoEvent;

    @Mock
    private DatahubService datahubService;

    @Mock
    private DatahubUpgradeCcmPollerService upgradeCcmPollerService;

    @Captor
    private ArgumentCaptor<BaseNamedFlowEvent> baseNamedFlowEvent;

    @InjectMocks
    private UpgradeCcmOnDatahubHandler underTest;

    @BeforeEach
    void setUp() {
        lenient().when(mockEnvironmentDtoEvent.getHeaders()).thenReturn(mockEventHeaders);
        lenient().when(mockEnvironmentDtoEvent.getData()).thenReturn(mockEnvironmentDto);

        lenient().when(mockEnvironmentDto.getAccountId()).thenReturn(TEST_ACCOUNT_ID);
        lenient().when(mockEnvironmentDto.getResourceCrn()).thenReturn(TEST_ENV_CRN);
        lenient().when(mockEnvironmentDto.getName()).thenReturn(TEST_ENV_NAME);
        lenient().when(mockEnvironmentDto.getId()).thenReturn(TEST_ENV_ID);
        lenient().when(mockEnvironmentDto.getResourceId()).thenReturn(TEST_ENV_ID);
        lenient().doAnswer(i -> null).when(mockEventSender).sendEvent(baseNamedFlowEvent.capture(), any(Event.Headers.class));
    }

    @Test
    void testAcceptWhenUpgradeErrorOnAllDatahubs() {
        StackViewV4Response stackResponse1 = new StackViewV4Response();
        stackResponse1.setCrn(TEST_STACK1_CRN);
        StackViewV4Response stackResponse2 = new StackViewV4Response();
        stackResponse2.setCrn(TEST_STACK2_CRN);
        StackViewV4Responses stackResponses = new StackViewV4Responses(Set.of(stackResponse1, stackResponse2));
        when(datahubService.list(TEST_ENV_CRN)).thenReturn(stackResponses);

        DistroXCcmUpgradeV1Response response1 =
                new DistroXCcmUpgradeV1Response(CcmUpgradeResponseType.ERROR, new FlowIdentifier(FlowType.FLOW, "flowId"), "reason", TEST_STACK1_CRN);
        when(datahubService.upgradeCcm(TEST_STACK1_CRN)).thenReturn(response1);
        DistroXCcmUpgradeV1Response response2 =
                new DistroXCcmUpgradeV1Response(CcmUpgradeResponseType.ERROR, new FlowIdentifier(FlowType.FLOW, "flowId"), "reason", TEST_STACK2_CRN);
        when(datahubService.upgradeCcm(TEST_STACK2_CRN)).thenReturn(response2);

        underTest.accept(mockEnvironmentDtoEvent);

        verify(upgradeCcmPollerService, never()).waitForUpgradeOnFlowIds(any(), any());
        assertThat(baseNamedFlowEvent.getValue()).isInstanceOf(UpgradeCcmFailedEvent.class);
        UpgradeCcmFailedEvent capturedUpgradeCcmEvent = (UpgradeCcmFailedEvent) baseNamedFlowEvent.getValue();
        assertThat(capturedUpgradeCcmEvent.getResourceName()).isEqualTo(TEST_ENV_NAME);
        assertThat(capturedUpgradeCcmEvent.getResourceId()).isEqualTo(TEST_ENV_ID);
        assertThat(capturedUpgradeCcmEvent.getResourceCrn()).isEqualTo(TEST_ENV_CRN);
        assertThat(capturedUpgradeCcmEvent.selector()).isEqualTo("FAILED_UPGRADE_CCM_EVENT");
        assertThat(capturedUpgradeCcmEvent.getEnvironmentStatus()).isEqualTo(UPGRADE_CCM_ON_DATAHUB_FAILED);
    }

    @Test
    void testAcceptWhenUpgradeErrorOnOneDatahub() {
        StackViewV4Response stackResponse1 = new StackViewV4Response();
        stackResponse1.setCrn(TEST_STACK1_CRN);
        StackViewV4Response stackResponse2 = new StackViewV4Response();
        stackResponse2.setCrn(TEST_STACK2_CRN);
        StackViewV4Responses stackResponses = new StackViewV4Responses(Set.of(stackResponse1, stackResponse2));
        when(datahubService.list(TEST_ENV_CRN)).thenReturn(stackResponses);

        FlowIdentifier flowId1 = new FlowIdentifier(FlowType.FLOW, "flowId1");
        DistroXCcmUpgradeV1Response response1 =
                new DistroXCcmUpgradeV1Response(CcmUpgradeResponseType.TRIGGERED, flowId1, "reason", TEST_STACK1_CRN);
        when(datahubService.upgradeCcm(TEST_STACK1_CRN)).thenReturn(response1);
        FlowIdentifier flowId2 = new FlowIdentifier(FlowType.FLOW, "flowId2");
        DistroXCcmUpgradeV1Response response2 =
                new DistroXCcmUpgradeV1Response(CcmUpgradeResponseType.ERROR, flowId2, "reason", TEST_STACK2_CRN);
        when(datahubService.upgradeCcm(TEST_STACK2_CRN)).thenReturn(response2);

        doThrow(new DatahubOperationFailedException("error")).when(upgradeCcmPollerService).waitForUpgradeOnFlowIds(TEST_ENV_ID, List.of(flowId1));

        underTest.accept(mockEnvironmentDtoEvent);
        verify(upgradeCcmPollerService).waitForUpgradeOnFlowIds(TEST_ENV_ID, List.of(flowId1));
        assertThat(baseNamedFlowEvent.getValue()).isInstanceOf(UpgradeCcmFailedEvent.class);
        UpgradeCcmFailedEvent capturedUpgradeCcmEvent = (UpgradeCcmFailedEvent) baseNamedFlowEvent.getValue();
        assertThat(capturedUpgradeCcmEvent.getResourceName()).isEqualTo(TEST_ENV_NAME);
        assertThat(capturedUpgradeCcmEvent.getResourceId()).isEqualTo(TEST_ENV_ID);
        assertThat(capturedUpgradeCcmEvent.getResourceCrn()).isEqualTo(TEST_ENV_CRN);
        assertThat(capturedUpgradeCcmEvent.selector()).isEqualTo("FAILED_UPGRADE_CCM_EVENT");
        assertThat(capturedUpgradeCcmEvent.getEnvironmentStatus()).isEqualTo(UPGRADE_CCM_ON_DATAHUB_FAILED);
    }

    @Test
    void testAcceptWhenUpgradeSkipped() {
        StackViewV4Response stackResponse1 = new StackViewV4Response();
        stackResponse1.setCrn(TEST_STACK1_CRN);
        StackViewV4Responses stackResponses = new StackViewV4Responses(Set.of(stackResponse1));
        when(datahubService.list(TEST_ENV_CRN)).thenReturn(stackResponses);

        FlowIdentifier flowId = new FlowIdentifier(FlowType.FLOW, "flowId");
        DistroXCcmUpgradeV1Response response =
                new DistroXCcmUpgradeV1Response(CcmUpgradeResponseType.SKIP, flowId, "reason", TEST_STACK1_CRN);

        when(datahubService.upgradeCcm(TEST_STACK1_CRN)).thenReturn(response);

        underTest.accept(mockEnvironmentDtoEvent);

        verify(upgradeCcmPollerService).waitForUpgradeOnFlowIds(TEST_ENV_ID, List.of());
        assertThat(baseNamedFlowEvent.getValue()).isInstanceOf(UpgradeCcmEvent.class);
        UpgradeCcmEvent capturedUpgradeCcmEvent = (UpgradeCcmEvent) baseNamedFlowEvent.getValue();
        assertThat(capturedUpgradeCcmEvent.getResourceName()).isEqualTo(TEST_ENV_NAME);
        assertThat(capturedUpgradeCcmEvent.getResourceId()).isEqualTo(TEST_ENV_ID);
        assertThat(capturedUpgradeCcmEvent.getResourceCrn()).isEqualTo(TEST_ENV_CRN);
        assertThat(capturedUpgradeCcmEvent.selector()).isEqualTo("FINISH_UPGRADE_CCM_EVENT");
    }

    @Test
    void testAcceptWhenUpgradeSkippedOnOneDatahub() {
        StackViewV4Response stackResponse1 = new StackViewV4Response();
        stackResponse1.setCrn(TEST_STACK1_CRN);
        StackViewV4Response stackResponse2 = new StackViewV4Response();
        stackResponse2.setCrn(TEST_STACK2_CRN);
        StackViewV4Responses stackResponses = new StackViewV4Responses(Set.of(stackResponse1, stackResponse2));
        when(datahubService.list(TEST_ENV_CRN)).thenReturn(stackResponses);

        FlowIdentifier flowId1 = new FlowIdentifier(FlowType.FLOW, "flowId1");
        DistroXCcmUpgradeV1Response response1 =
                new DistroXCcmUpgradeV1Response(CcmUpgradeResponseType.SKIP, flowId1, "reason", TEST_STACK1_CRN);
        when(datahubService.upgradeCcm(TEST_STACK1_CRN)).thenReturn(response1);
        FlowIdentifier flowId2 = new FlowIdentifier(FlowType.FLOW, "flowId2");
        DistroXCcmUpgradeV1Response response2 =
                new DistroXCcmUpgradeV1Response(CcmUpgradeResponseType.TRIGGERED, flowId2, "reason", TEST_STACK2_CRN);
        when(datahubService.upgradeCcm(TEST_STACK2_CRN)).thenReturn(response2);

        underTest.accept(mockEnvironmentDtoEvent);

        verify(upgradeCcmPollerService).waitForUpgradeOnFlowIds(TEST_ENV_ID, List.of(flowId2));
        assertThat(baseNamedFlowEvent.getValue()).isInstanceOf(UpgradeCcmEvent.class);
        UpgradeCcmEvent capturedUpgradeCcmEvent = (UpgradeCcmEvent) baseNamedFlowEvent.getValue();
        assertThat(capturedUpgradeCcmEvent.getResourceName()).isEqualTo(TEST_ENV_NAME);
        assertThat(capturedUpgradeCcmEvent.getResourceId()).isEqualTo(TEST_ENV_ID);
        assertThat(capturedUpgradeCcmEvent.getResourceCrn()).isEqualTo(TEST_ENV_CRN);
        assertThat(capturedUpgradeCcmEvent.selector()).isEqualTo("FINISH_UPGRADE_CCM_EVENT");
    }

    @Test
    void testAcceptWhenUpgradeTriggered() {
        StackViewV4Response stackResponse = new StackViewV4Response();
        stackResponse.setCrn(TEST_STACK1_CRN);
        StackViewV4Responses stackResponses = new StackViewV4Responses(Set.of(stackResponse));
        when(datahubService.list(TEST_ENV_CRN)).thenReturn(stackResponses);

        FlowIdentifier flowId = new FlowIdentifier(FlowType.FLOW, "flowId");
        DistroXCcmUpgradeV1Response response =
                new DistroXCcmUpgradeV1Response(CcmUpgradeResponseType.TRIGGERED, flowId, "reason", TEST_STACK1_CRN);
        when(datahubService.upgradeCcm(TEST_STACK1_CRN)).thenReturn(response);

        underTest.accept(mockEnvironmentDtoEvent);

        verify(upgradeCcmPollerService).waitForUpgradeOnFlowIds(TEST_ENV_ID, List.of(flowId));
        assertThat(baseNamedFlowEvent.getValue()).isInstanceOf(UpgradeCcmEvent.class);
        UpgradeCcmEvent capturedUpgradeCcmEvent = (UpgradeCcmEvent) baseNamedFlowEvent.getValue();
        assertThat(capturedUpgradeCcmEvent.getResourceName()).isEqualTo(TEST_ENV_NAME);
        assertThat(capturedUpgradeCcmEvent.getResourceId()).isEqualTo(TEST_ENV_ID);
        assertThat(capturedUpgradeCcmEvent.getResourceCrn()).isEqualTo(TEST_ENV_CRN);
        assertThat(capturedUpgradeCcmEvent.selector()).isEqualTo("FINISH_UPGRADE_CCM_EVENT");
    }

    @Test
    void testAcceptWhenNoDistroX() {
        when(datahubService.list(TEST_ENV_CRN)).thenReturn(new StackViewV4Responses());

        underTest.accept(mockEnvironmentDtoEvent);

        verify(datahubService, never()).upgradeCcm(any());
        verify(upgradeCcmPollerService, never()).waitForUpgradeOnFlowIds(any(), any());
        assertThat(baseNamedFlowEvent.getValue()).isInstanceOf(UpgradeCcmEvent.class);
        UpgradeCcmEvent capturedUpgradeCcmEvent = (UpgradeCcmEvent) baseNamedFlowEvent.getValue();
        assertThat(capturedUpgradeCcmEvent.getResourceName()).isEqualTo(TEST_ENV_NAME);
        assertThat(capturedUpgradeCcmEvent.getResourceId()).isEqualTo(TEST_ENV_ID);
        assertThat(capturedUpgradeCcmEvent.getResourceCrn()).isEqualTo(TEST_ENV_CRN);
        assertThat(capturedUpgradeCcmEvent.selector()).isEqualTo("FINISH_UPGRADE_CCM_EVENT");
    }

    @Test
    void selector() {
        assertEquals(UPGRADE_CCM_DATAHUB_HANDLER.name(), underTest.selector());
    }
}
