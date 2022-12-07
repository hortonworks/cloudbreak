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

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationDefaultEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationFailedEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors;
import com.sequenceiq.environment.environment.service.sdx.SdxPollerService;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@ExtendWith(MockitoExtension.class)
class ProxyModificationOnDatalakeHandlerTest {

    private static final String SELECTOR = "selector";

    private static final long ENV_ID = 1L;

    private static final String NAME = "name";

    private static final String PREV_PROXY_CRN = "prev-proxy-crn";

    @Mock
    private SdxPollerService sdxPollerService;

    @Mock
    private EventSender eventSender;

    @InjectMocks
    private ProxyModificationOnDatalakeHandler underTest;

    @Mock
    private EnvironmentDto environmentDto;

    @Mock
    private ProxyConfig proxyConfig;

    @Mock
    private ProxyConfig previousProxyConfig;

    private EnvProxyModificationDefaultEvent event;

    @BeforeEach
    void setUp() {
        event = EnvProxyModificationDefaultEvent.builder()
                .withSelector(SELECTOR)
                .withEnvironmentDto(environmentDto)
                .withProxyConfig(proxyConfig)
                .withPreviousProxyConfig(previousProxyConfig)
                .build();
        lenient().when(environmentDto.getId()).thenReturn(ENV_ID);
        lenient().when(environmentDto.getName()).thenReturn(NAME);
        lenient().when(previousProxyConfig.getResourceCrn()).thenReturn(PREV_PROXY_CRN);
    }

    @Test
    void failure() {
        RuntimeException cause = new RuntimeException("cause");
        doThrow(cause).when(sdxPollerService).modifyProxyConfigOnAttachedDatalakeClusters(ENV_ID, NAME, PREV_PROXY_CRN);
        Event<EnvProxyModificationDefaultEvent> wrappedEvent = Event.wrap(event);

        underTest.accept(wrappedEvent);

        EnvProxyModificationFailedEvent failedEvent = EnvProxyModificationFailedEvent.builder()
                .withEnvironmentDto(event.getEnvironmentDto())
                .withProxyConfig(event.getProxyConfig())
                .withPreviousProxyConfig(event.getPreviousProxyConfig())
                .withEnvironmentStatus(EnvironmentStatus.PROXY_CONFIG_MODIFICATION_ON_DATALAKE_FAILED)
                .withException(cause)
                .build();
        verify(eventSender).sendEvent(failedEvent, wrappedEvent.getHeaders());
    }

    @Test
    void success() {
        Event<EnvProxyModificationDefaultEvent> wrappedEvent = Event.wrap(event);

        underTest.accept(wrappedEvent);

        EnvProxyModificationDefaultEvent defaultEvent = EnvProxyModificationDefaultEvent.builder()
                .withSelector(EnvProxyModificationStateSelectors.FINISH_MODIFY_PROXY_EVENT.selector())
                .withEnvironmentDto(event.getEnvironmentDto())
                .withProxyConfig(event.getProxyConfig())
                .withPreviousProxyConfig(event.getPreviousProxyConfig())
                .build();
        verify(eventSender).sendEvent(defaultEvent, wrappedEvent.getHeaders());
    }

}
