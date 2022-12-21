package com.sequenceiq.environment.environment.flow.modify.proxy.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationDefaultEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationFailedEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@ExtendWith(MockitoExtension.class)
class ProxyModificationOnFreeipaHandlerTest {

    private static final String SELECTOR = "selector";

    private static final long ENV_ID = 1L;

    private static final String CRN = "crn";

    private static final String PROXY_CONFIG_CRN = "proxy-crn";

    private static final String PREV_PROXY_CRN = "prev-proxy-crn";

    @Mock
    private FreeIpaPollerService freeIpaPollerService;

    @Mock
    private EventSender eventSender;

    @InjectMocks
    private ProxyModificationOnFreeipaHandler underTest;

    @Mock
    private EnvironmentDto environmentDto;

    @Captor
    private ArgumentCaptor<EnvProxyModificationDefaultEvent> defaultEventCaptor;

    @Captor
    private ArgumentCaptor<EnvProxyModificationFailedEvent> failedEventCaptor;

    private EnvProxyModificationDefaultEvent event;

    @BeforeEach
    void setUp() {
        event = EnvProxyModificationDefaultEvent.builder()
                .withSelector(SELECTOR)
                .withResourceId(ENV_ID)
                .withResourceCrn(CRN)
                .withProxyConfigCrn(PROXY_CONFIG_CRN)
                .withPreviousProxyConfigCrn(PREV_PROXY_CRN)
                .build();
        lenient().when(environmentDto.getId()).thenReturn(ENV_ID);
        lenient().when(environmentDto.getResourceCrn()).thenReturn(CRN);
    }

    @Test
    void failure() {
        RuntimeException cause = new RuntimeException("cause");
        doThrow(cause).when(freeIpaPollerService).waitForModifyProxyConfig(ENV_ID, CRN, PREV_PROXY_CRN);
        Event<EnvProxyModificationDefaultEvent> wrappedEvent = Event.wrap(event);

        underTest.accept(wrappedEvent);

        verify(eventSender).sendEvent(failedEventCaptor.capture(), eq(wrappedEvent.getHeaders()));
        assertThat(failedEventCaptor.getValue())
                .returns(cause, BaseFailedFlowEvent::getException)
                .returns(EnvironmentStatus.PROXY_CONFIG_MODIFICATION_ON_FREEIPA_FAILED, EnvProxyModificationFailedEvent::getEnvironmentStatus);
    }

    @Test
    void success() {
        Event<EnvProxyModificationDefaultEvent> wrappedEvent = Event.wrap(event);

        underTest.accept(wrappedEvent);

        verify(eventSender).sendEvent(defaultEventCaptor.capture(), eq(wrappedEvent.getHeaders()));
        assertThat(defaultEventCaptor.getValue())
                .returns(EnvProxyModificationStateSelectors.MODIFY_PROXY_DATALAKE_EVENT.selector(), Selectable::getSelector);
    }

}