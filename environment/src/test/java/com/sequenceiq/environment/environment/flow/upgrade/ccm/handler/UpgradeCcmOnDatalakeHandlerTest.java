package com.sequenceiq.environment.environment.flow.upgrade.ccm.handler;

import static com.sequenceiq.environment.environment.EnvironmentStatus.UPGRADE_CCM_ON_DATALAKE_FAILED;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmHandlerSelectors.UPGRADE_CCM_DATALAKE_HANDLER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmEvent;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmFailedEvent;
import com.sequenceiq.environment.environment.service.sdx.SdxService;
import com.sequenceiq.environment.environment.service.sdx.SdxUpgradeCcmPollerService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.sdx.api.model.CcmUpgradeResponseType;
import com.sequenceiq.sdx.api.model.SdxCcmUpgradeResponse;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
class UpgradeCcmOnDatalakeHandlerTest {

    private static final String TEST_ACCOUNT_ID = "someAccountId";

    private static final String TEST_ENV_CRN = "someEnvCrn";

    private static final String TEST_ENV_NAME = "someEnvName";

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
    private SdxService sdxService;

    @Mock
    private SdxUpgradeCcmPollerService sdxUpgradeCcmPollerService;

    @Captor
    private ArgumentCaptor<BaseNamedFlowEvent> baseNamedFlowEvent;

    @InjectMocks
    private UpgradeCcmOnDatalakeHandler underTest;

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
    void testAcceptWhenUpgradeError() {
        SdxCcmUpgradeResponse response =
                new SdxCcmUpgradeResponse(CcmUpgradeResponseType.ERROR, new FlowIdentifier(FlowType.FLOW, "flowId"), "reason", "datalakeCrn");
        when(sdxService.upgradeCcm(TEST_ENV_CRN)).thenReturn(response);
        underTest.accept(mockEnvironmentDtoEvent);
        verify(sdxUpgradeCcmPollerService, never()).waitForUpgradeCcm(any(), any());
        UpgradeCcmFailedEvent capturedUpgradeCcmEvent = (UpgradeCcmFailedEvent) baseNamedFlowEvent.getValue();
        assertThat(capturedUpgradeCcmEvent.getResourceName()).isEqualTo(TEST_ENV_NAME);
        assertThat(capturedUpgradeCcmEvent.getResourceId()).isEqualTo(TEST_ENV_ID);
        assertThat(capturedUpgradeCcmEvent.getResourceCrn()).isEqualTo(TEST_ENV_CRN);
        assertThat(capturedUpgradeCcmEvent.selector()).isEqualTo("FAILED_UPGRADE_CCM_EVENT");
        assertThat(capturedUpgradeCcmEvent.getEnvironmentStatus()).isEqualTo(UPGRADE_CCM_ON_DATALAKE_FAILED);
    }

    @Test
    void testAcceptWhenUpgradeSkipped() {
        SdxCcmUpgradeResponse response =
                new SdxCcmUpgradeResponse(CcmUpgradeResponseType.SKIP, new FlowIdentifier(FlowType.FLOW, "flowId"), "reason", "datalakeCrn");
        when(sdxService.upgradeCcm(TEST_ENV_CRN)).thenReturn(response);
        underTest.accept(mockEnvironmentDtoEvent);
        verify(sdxUpgradeCcmPollerService, never()).waitForUpgradeCcm(any(), any());
        UpgradeCcmEvent capturedUpgradeCcmEvent = (UpgradeCcmEvent) baseNamedFlowEvent.getValue();
        assertThat(capturedUpgradeCcmEvent.getResourceName()).isEqualTo(TEST_ENV_NAME);
        assertThat(capturedUpgradeCcmEvent.getResourceId()).isEqualTo(TEST_ENV_ID);
        assertThat(capturedUpgradeCcmEvent.getResourceCrn()).isEqualTo(TEST_ENV_CRN);
        assertThat(capturedUpgradeCcmEvent.selector()).isEqualTo("UPGRADE_CCM_DATAHUB_EVENT");
    }

    @Test
    void testAcceptWhenUpgradeTriggered() {
        SdxCcmUpgradeResponse response =
                new SdxCcmUpgradeResponse(CcmUpgradeResponseType.TRIGGERED, new FlowIdentifier(FlowType.FLOW, "flowId"), "reason", "datalakeCrn");
        when(sdxService.upgradeCcm(TEST_ENV_CRN)).thenReturn(response);
        underTest.accept(mockEnvironmentDtoEvent);
        verify(sdxUpgradeCcmPollerService).waitForUpgradeCcm(TEST_ENV_ID, "datalakeCrn");
        UpgradeCcmEvent capturedUpgradeCcmEvent = (UpgradeCcmEvent) baseNamedFlowEvent.getValue();
        assertThat(capturedUpgradeCcmEvent.getResourceName()).isEqualTo(TEST_ENV_NAME);
        assertThat(capturedUpgradeCcmEvent.getResourceId()).isEqualTo(TEST_ENV_ID);
        assertThat(capturedUpgradeCcmEvent.getResourceCrn()).isEqualTo(TEST_ENV_CRN);
        assertThat(capturedUpgradeCcmEvent.selector()).isEqualTo("UPGRADE_CCM_DATAHUB_EVENT");
    }

    @Test
    void selector() {
        assertEquals(UPGRADE_CCM_DATALAKE_HANDLER.name(), underTest.selector());
    }

}
