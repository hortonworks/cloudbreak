package com.sequenceiq.cloudbreak.reactor.handler;

import static org.mockito.ArgumentMatchers.any;
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
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpdateDomainDnsResolverRequest;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.TargetedUpscaleSupportService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
public class UpdateDomainDnsResolverHandlerTest {

    @Mock
    private EventBus eventBus;

    @Mock
    private StackService stackService;

    @Mock
    private TargetedUpscaleSupportService targetedUpscaleSupportService;

    @InjectMocks
    private UpdateDomainDnsResolverHandler underTest;

    @Test
    public void testUpdateDuringUpscale() {
        when(stackService.getByIdWithLists(any())).thenReturn(stack(DnsResolverType.FREEIPA_FOR_ENV));
        when(targetedUpscaleSupportService.getActualDnsResolverType(any())).thenReturn(DnsResolverType.LOCAL_UNBOUND);
        when(stackService.save(any())).thenReturn(new Stack());

        underTest.accept(new Event<>(new UpdateDomainDnsResolverRequest(1L)));

        verify(stackService, times(1)).getByIdWithLists(any());
        verify(stackService, times(1)).save(any());
        verify(targetedUpscaleSupportService, times(1)).getActualDnsResolverType(any());
        verify(eventBus, times(1)).notify(any(Object.class), any(Event.class));
    }

    @Test
    public void testSkipUpdateDuringUpscale() {
        when(stackService.getByIdWithLists(any())).thenReturn(stack(DnsResolverType.FREEIPA_FOR_ENV));
        when(targetedUpscaleSupportService.getActualDnsResolverType(any())).thenReturn(DnsResolverType.FREEIPA_FOR_ENV);

        underTest.accept(new Event<>(new UpdateDomainDnsResolverRequest(1L)));

        verify(stackService, times(1)).getByIdWithLists(any());
        verify(stackService, times(0)).save(any());
        verify(targetedUpscaleSupportService, times(1)).getActualDnsResolverType(any());
        verify(eventBus, times(1)).notify(any(Object.class), any(Event.class));
    }

    @Test
    public void testUpdateIfThereIsErrorDuringUpdate() {
        when(stackService.getByIdWithLists(any())).thenReturn(new Stack());
        when(targetedUpscaleSupportService.getActualDnsResolverType(any())).thenThrow(new RuntimeException("whatever"));

        underTest.accept(new Event<>(new UpdateDomainDnsResolverRequest(1L)));

        verify(stackService, times(1)).getByIdWithLists(any());
        verify(stackService, times(0)).save(any());
        verify(targetedUpscaleSupportService, times(1)).getActualDnsResolverType(any());
        verify(eventBus, times(1)).notify(any(Object.class), any(Event.class));
    }

    private Stack stack(DnsResolverType dnsResolverType) {
        Stack stack = new Stack();
        stack.setDomainDnsResolver(dnsResolverType);
        return stack;
    }
}
