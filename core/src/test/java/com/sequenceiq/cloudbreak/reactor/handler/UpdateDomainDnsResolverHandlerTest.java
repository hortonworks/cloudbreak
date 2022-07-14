package com.sequenceiq.cloudbreak.reactor.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.stack.DnsResolverType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpdateDomainDnsResolverRequest;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.TargetedUpscaleSupportService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
public class UpdateDomainDnsResolverHandlerTest {

    @Mock
    private EventBus eventBus;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private TargetedUpscaleSupportService targetedUpscaleSupportService;

    @InjectMocks
    private UpdateDomainDnsResolverHandler underTest;

    @Test
    public void testUpdateDuringUpscale() {
        StackDto stack = stack(DnsResolverType.FREEIPA_FOR_ENV);
        when(stackDtoService.getById(any())).thenReturn(stack);
        when(targetedUpscaleSupportService.getActualDnsResolverType(any())).thenReturn(DnsResolverType.LOCAL_UNBOUND);

        underTest.accept(new Event<>(new UpdateDomainDnsResolverRequest(1L)));

        verify(stackDtoService, times(1)).getById(any());
        verify(stackDtoService, times(0)).updateDomainDnsResolver(anyLong(), eq(DnsResolverType.FREEIPA_FOR_ENV));
        verify(targetedUpscaleSupportService, times(1)).getActualDnsResolverType(any());
        verify(eventBus, times(1)).notify(any(Object.class), any(Event.class));
    }

    @Test
    public void testSkipUpdateDuringUpscale() {
        StackDto stack = stack(DnsResolverType.FREEIPA_FOR_ENV);
        when(stackDtoService.getById(any())).thenReturn(stack);
        when(targetedUpscaleSupportService.getActualDnsResolverType(any())).thenReturn(DnsResolverType.FREEIPA_FOR_ENV);

        underTest.accept(new Event<>(new UpdateDomainDnsResolverRequest(1L)));

        verify(stackDtoService, times(1)).getById(any());
        verify(stackDtoService, times(0)).updateDomainDnsResolver(anyLong(), eq(DnsResolverType.FREEIPA_FOR_ENV));
        verify(targetedUpscaleSupportService, times(1)).getActualDnsResolverType(any());
        verify(eventBus, times(1)).notify(any(Object.class), any(Event.class));
    }

    @Test
    public void testUpdateIfThereIsErrorDuringUpdate() {
        StackDto stack = stack(DnsResolverType.UNKNOWN);
        when(stackDtoService.getById(any())).thenReturn(stack);
        when(targetedUpscaleSupportService.getActualDnsResolverType(any())).thenThrow(new RuntimeException("whatever"));

        underTest.accept(new Event<>(new UpdateDomainDnsResolverRequest(1L)));

        verify(stackDtoService, times(1)).getById(any());
        verify(stackDtoService, times(0)).updateDomainDnsResolver(anyLong(), eq(DnsResolverType.FREEIPA_FOR_ENV));
        verify(targetedUpscaleSupportService, times(1)).getActualDnsResolverType(any());
        verify(eventBus, times(1)).notify(any(Object.class), any(Event.class));
    }

    private StackDto stack(DnsResolverType dnsResolverType) {
        StackDto stackDto = mock(StackDto.class);
        Stack stack = new Stack();
        stack.setDomainDnsResolver(dnsResolverType);
        when(stackDto.getStack()).thenReturn(stack);
        return stackDto;
    }
}
