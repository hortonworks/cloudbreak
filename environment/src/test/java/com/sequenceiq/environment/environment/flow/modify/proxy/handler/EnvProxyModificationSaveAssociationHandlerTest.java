package com.sequenceiq.environment.environment.flow.modify.proxy.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationDefaultEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationFailedEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.flow.reactor.api.event.EventSender;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
class EnvProxyModificationSaveAssociationHandlerTest {

    private static final String SELECTOR = "selector";

    private static final long ENV_ID = 1L;

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

    private EnvProxyModificationDefaultEvent event;

    @BeforeEach
    void setUp() {
        event = new EnvProxyModificationDefaultEvent(SELECTOR, environmentDto, proxyConfig);
        lenient().when(environmentDto.getId()).thenReturn(ENV_ID);
        lenient().when(environmentService.findById(ENV_ID)).thenReturn(Optional.of(environmentDto));
    }

    @Test
    void acceptSuccess() {
        when(environmentService.updateProxyConfig(any(), any())).thenReturn(environmentDto);
        Event<EnvProxyModificationDefaultEvent> wrappedEvent = Event.wrap(event);
        underTest.accept(wrappedEvent);

        EnvProxyModificationDefaultEvent envProxyModificationEvent = new EnvProxyModificationDefaultEvent(
                EnvProxyModificationStateSelectors.FINISH_MODIFY_PROXY_EVENT.selector(), environmentDto, proxyConfig);
        verify(environmentService).updateProxyConfig(ENV_ID, proxyConfig);
        verify(eventSender).sendEvent(envProxyModificationEvent, wrappedEvent.getHeaders());
    }

    @Test
    void acceptFailure() {
        RuntimeException cause = new RuntimeException("cause");
        when(environmentService.updateProxyConfig(any(), any())).thenThrow(cause);
        Event<EnvProxyModificationDefaultEvent> wrappedEvent = Event.wrap(event);
        underTest.accept(wrappedEvent);

        EnvProxyModificationFailedEvent envProxyModificationFailedEvent = new EnvProxyModificationFailedEvent(
                environmentDto, EnvironmentStatus.PROXY_CONFIG_MODIFICATION_FAILED, cause);
        verify(environmentService).updateProxyConfig(ENV_ID, proxyConfig);
        verify(environmentService).findById(ENV_ID);
        verify(eventSender).sendEvent(envProxyModificationFailedEvent, wrappedEvent.getHeaders());
    }

}