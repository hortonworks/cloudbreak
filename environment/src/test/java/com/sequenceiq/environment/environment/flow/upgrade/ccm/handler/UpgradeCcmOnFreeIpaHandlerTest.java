package com.sequenceiq.environment.environment.flow.upgrade.ccm.handler;

import static com.sequenceiq.environment.environment.EnvironmentStatus.UPGRADE_CCM_ON_FREEIPA_FAILED;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmHandlerSelectors.UPGRADE_CCM_FREEIPA_HANDLER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmEvent;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmFailedEvent;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@ExtendWith(MockitoExtension.class)
class UpgradeCcmOnFreeIpaHandlerTest {

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
    private FreeIpaService freeIpaService;

    @Mock
    private FreeIpaPollerService freeIpaPollerService;

    @Captor
    private ArgumentCaptor<BaseNamedFlowEvent> baseNamedFlowEvent;

    @InjectMocks
    private UpgradeCcmOnFreeIpaHandler underTest;

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
    void selector() {
        assertEquals(UPGRADE_CCM_FREEIPA_HANDLER.name(), underTest.selector());
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = { "AVAILABLE", "UPGRADE_CCM_FAILED" }, mode = EnumSource.Mode.INCLUDE)
    void testFreeIpaAcceptedStatuses(Status status) {
        DescribeFreeIpaResponse freeipa = new DescribeFreeIpaResponse();
        freeipa.setStatus(status);
        freeipa.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        Optional<DescribeFreeIpaResponse> freeipaOpt = Optional.of(freeipa);
        when(freeIpaService.describe(any())).thenReturn(freeipaOpt);
        underTest.accept(mockEnvironmentDtoEvent);

        verify(freeIpaPollerService).waitForCcmUpgrade(TEST_ENV_ID, TEST_ENV_CRN);
        UpgradeCcmEvent capturedUpgradeCcmEvent = (UpgradeCcmEvent) baseNamedFlowEvent.getValue();
        assertThat(capturedUpgradeCcmEvent.getResourceName()).isEqualTo(TEST_ENV_NAME);
        assertThat(capturedUpgradeCcmEvent.getResourceId()).isEqualTo(TEST_ENV_ID);
        assertThat(capturedUpgradeCcmEvent.getResourceCrn()).isEqualTo(TEST_ENV_CRN);
        assertThat(capturedUpgradeCcmEvent.selector()).isEqualTo("UPGRADE_CCM_TUNNEL_UPDATE_EVENT");
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = { "AVAILABLE", "UPGRADE_CCM_FAILED" }, mode = EnumSource.Mode.EXCLUDE)
    void testFreeIpaWrongStatuses(Status status) {
        DescribeFreeIpaResponse freeipa = new DescribeFreeIpaResponse();
        freeipa.setStatus(status);
        freeipa.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        Optional<DescribeFreeIpaResponse> freeipaOpt = Optional.of(freeipa);
        when(freeIpaService.describe(any())).thenReturn(freeipaOpt);
        underTest.accept(mockEnvironmentDtoEvent);

        verify(freeIpaPollerService, never()).waitForCcmUpgrade(any(), any());
        UpgradeCcmFailedEvent capturedUpgradeCcmEvent = (UpgradeCcmFailedEvent) baseNamedFlowEvent.getValue();
        assertThat(capturedUpgradeCcmEvent.getResourceName()).isEqualTo(TEST_ENV_NAME);
        assertThat(capturedUpgradeCcmEvent.getResourceId()).isEqualTo(TEST_ENV_ID);
        assertThat(capturedUpgradeCcmEvent.getResourceCrn()).isEqualTo(TEST_ENV_CRN);
        assertThat(capturedUpgradeCcmEvent.selector()).isEqualTo("FAILED_UPGRADE_CCM_EVENT");
        assertThat(capturedUpgradeCcmEvent.getEnvironmentStatus()).isEqualTo(UPGRADE_CCM_ON_FREEIPA_FAILED);
    }

    @ParameterizedTest
    @EnumSource(Status.class)
    void testFreeIpaMissingAvailabilityStatus(Status status) {
        DescribeFreeIpaResponse freeipa = new DescribeFreeIpaResponse();
        freeipa.setStatus(status);
        freeipa.setAvailabilityStatus(null);
        Optional<DescribeFreeIpaResponse> freeipaOpt = Optional.of(freeipa);
        when(freeIpaService.describe(any())).thenReturn(freeipaOpt);
        underTest.accept(mockEnvironmentDtoEvent);

        verify(freeIpaPollerService, never()).waitForCcmUpgrade(any(), any());
        UpgradeCcmFailedEvent capturedUpgradeCcmEvent = (UpgradeCcmFailedEvent) baseNamedFlowEvent.getValue();
        assertThat(capturedUpgradeCcmEvent.getResourceName()).isEqualTo(TEST_ENV_NAME);
        assertThat(capturedUpgradeCcmEvent.getResourceId()).isEqualTo(TEST_ENV_ID);
        assertThat(capturedUpgradeCcmEvent.getResourceCrn()).isEqualTo(TEST_ENV_CRN);
        assertThat(capturedUpgradeCcmEvent.selector()).isEqualTo("FAILED_UPGRADE_CCM_EVENT");
        assertThat(capturedUpgradeCcmEvent.getEnvironmentStatus()).isEqualTo(UPGRADE_CCM_ON_FREEIPA_FAILED);
    }

    @Test
    void testFreeIpaDoesNotExist() {
        when(freeIpaService.describe(any())).thenReturn(Optional.empty());
        underTest.accept(mockEnvironmentDtoEvent);

        verify(freeIpaPollerService, never()).waitForCcmUpgrade(any(), any());
        UpgradeCcmFailedEvent capturedUpgradeCcmEvent = (UpgradeCcmFailedEvent) baseNamedFlowEvent.getValue();
        assertThat(capturedUpgradeCcmEvent.getResourceName()).isEqualTo(TEST_ENV_NAME);
        assertThat(capturedUpgradeCcmEvent.getResourceId()).isEqualTo(TEST_ENV_ID);
        assertThat(capturedUpgradeCcmEvent.getResourceCrn()).isEqualTo(TEST_ENV_CRN);
        assertThat(capturedUpgradeCcmEvent.selector()).isEqualTo("FAILED_UPGRADE_CCM_EVENT");
        assertThat(capturedUpgradeCcmEvent.getEnvironmentStatus()).isEqualTo(UPGRADE_CCM_ON_FREEIPA_FAILED);
    }
}
