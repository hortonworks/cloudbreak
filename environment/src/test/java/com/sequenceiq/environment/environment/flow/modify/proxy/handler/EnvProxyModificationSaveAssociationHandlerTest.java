package com.sequenceiq.environment.environment.flow.modify.proxy.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Optional;

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
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.environment.proxy.service.ProxyConfigService;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@ExtendWith(MockitoExtension.class)
class EnvProxyModificationSaveAssociationHandlerTest {

    private static final String SELECTOR = "selector";

    private static final long ENV_ID = 1L;

    private static final String PROXY_CONFIG_CRN = "proxy-crn";

    @Mock
    private ProxyConfigService proxyConfigService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EventSender eventSender;

    @InjectMocks
    private EnvProxyModificationSaveAssociationHandler underTest;

    @Mock
    private EnvironmentDto environmentDto;

    @Mock
    private ProxyConfig proxyConfig;

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
                .withProxyConfigCrn(PROXY_CONFIG_CRN)
                .build();
        lenient().when(environmentDto.getId()).thenReturn(ENV_ID);
        lenient().when(environmentService.findById(ENV_ID)).thenReturn(Optional.of(environmentDto));
        lenient().when(proxyConfigService.getByCrn(PROXY_CONFIG_CRN)).thenReturn(proxyConfig);
    }

    @Test
    void acceptSuccess() {
        Event<EnvProxyModificationDefaultEvent> wrappedEvent = Event.wrap(event);
        underTest.accept(wrappedEvent);

        verify(environmentService).updateProxyConfig(ENV_ID, proxyConfig);
        verify(eventSender).sendEvent(defaultEventCaptor.capture(), eq(wrappedEvent.getHeaders()));
        assertThat(defaultEventCaptor.getValue())
                .returns(EnvProxyModificationStateSelectors.MODIFY_PROXY_FREEIPA_EVENT.selector(), Selectable::getSelector);
    }

    @Test
    void acceptSuccessWithNoProxy() {
        event = EnvProxyModificationDefaultEvent.builder()
                .withSelector(SELECTOR)
                .withResourceId(ENV_ID)
                .withProxyConfigCrn(null)
                .build();
        Event<EnvProxyModificationDefaultEvent> wrappedEvent = Event.wrap(event);
        underTest.accept(wrappedEvent);

        verify(environmentService).updateProxyConfig(ENV_ID, null);
        verifyNoInteractions(proxyConfigService);
        verify(eventSender).sendEvent(defaultEventCaptor.capture(), eq(wrappedEvent.getHeaders()));
        assertThat(defaultEventCaptor.getValue())
                .returns(EnvProxyModificationStateSelectors.MODIFY_PROXY_FREEIPA_EVENT.selector(), Selectable::getSelector);
    }

    @Test
    void acceptFailure() {
        RuntimeException cause = new RuntimeException("cause");
        doThrow(cause).when(environmentService).updateProxyConfig(any(), any());
        Event<EnvProxyModificationDefaultEvent> wrappedEvent = Event.wrap(event);
        underTest.accept(wrappedEvent);

        verify(environmentService).updateProxyConfig(ENV_ID, proxyConfig);
        verify(eventSender).sendEvent(failedEventCaptor.capture(), eq(wrappedEvent.getHeaders()));
        assertThat(failedEventCaptor.getValue())
                .returns(cause, BaseFailedFlowEvent::getException)
                .returns(EnvironmentStatus.PROXY_CONFIG_MODIFICATION_FAILED, EnvProxyModificationFailedEvent::getEnvironmentStatus);
    }

}