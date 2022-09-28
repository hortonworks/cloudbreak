package com.sequenceiq.environment.environment.flow.modify.proxy.handler;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

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
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.flow.reactor.api.event.EventSender;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
class ProxyModificationOnFreeipaHandlerTest {

    private static final String SELECTOR = "selector";

    private static final long ENV_ID = 1L;

    private static final String CRN = "crn";

    @Mock
    private FreeIpaPollerService freeIpaPollerService;

    @Mock
    private EventSender eventSender;

    @InjectMocks
    private ProxyModificationOnFreeipaHandler underTest;

    @Mock
    private EnvironmentDto environmentDto;

    @Mock
    private ProxyConfig proxyConfig;

    private EnvProxyModificationDefaultEvent event;

    @BeforeEach
    void setUp() {
        event = new EnvProxyModificationDefaultEvent(SELECTOR, environmentDto, proxyConfig);
        lenient().when(environmentDto.getId()).thenReturn(ENV_ID);
        lenient().when(environmentDto.getResourceCrn()).thenReturn(CRN);
    }

    @Test
    void failure() {
        RuntimeException cause = new RuntimeException("cause");
        doThrow(cause).when(freeIpaPollerService).waitForModifyProxyConfig(ENV_ID, CRN);
        Event<EnvProxyModificationDefaultEvent> wrappedEvent = Event.wrap(event);

        underTest.accept(wrappedEvent);

        EnvProxyModificationFailedEvent failedEvent = new EnvProxyModificationFailedEvent(
                event.getEnvironmentDto(), event.getProxyConfig(), EnvironmentStatus.PROXY_CONFIG_MODIFICATION_ON_FREEIPA_FAILED, cause);
        verify(eventSender).sendEvent(failedEvent, wrappedEvent.getHeaders());
    }

    @Test
    void success() {
        Event<EnvProxyModificationDefaultEvent> wrappedEvent = Event.wrap(event);

        underTest.accept(wrappedEvent);

        EnvProxyModificationDefaultEvent defaultEvent = new EnvProxyModificationDefaultEvent(
                EnvProxyModificationStateSelectors.FINISH_MODIFY_PROXY_EVENT.selector(), event.getEnvironmentDto(), event.getProxyConfig());
        verify(eventSender).sendEvent(defaultEvent, wrappedEvent.getHeaders());
    }

}