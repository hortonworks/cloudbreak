package com.sequenceiq.cloudbreak.reactor.handler.cluster.update.publicdns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.update.publicdns.UpdatePublicDnsEntriesFlowEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.update.publicdns.UpdatePublicDnsEntriesInPemFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.update.publicdns.UpdatePublicDnsEntriesInPemFinished;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.update.publicdns.UpdatePublicDnsEntriesInPemRequest;
import com.sequenceiq.cloudbreak.service.publicendpoint.ClusterPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
class UpdatePublicDnsEntriesInPemHandlerTest {

    @Mock
    private ClusterPublicEndpointManagementService clusterPublicEndpointManagementService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private UpdatePublicDnsEntriesInPemHandler underTest;

    @Test
    void testAcceptWhenDNSUpdateCouldNotBeFinished() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getById(1L)).thenReturn(stackDto);
        doThrow(new CloudbreakServiceException("Something bad happened"))
                .when(clusterPublicEndpointManagementService).refreshDnsEntries(stackDto);

        underTest.accept(new Event<>(new UpdatePublicDnsEntriesInPemRequest(1L)));

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(stringArgumentCaptor.capture(), eventArgumentCaptor.capture());

        assertEquals(UpdatePublicDnsEntriesFlowEvent.UPDATE_PUBLIC_DNS_ENTRIES_FAILED_EVENT.event(), stringArgumentCaptor.getValue());
        assertEquals(UpdatePublicDnsEntriesInPemFailed.class, eventArgumentCaptor.getValue().getData().getClass());
    }

    @Test
    void testAcceptWhenDNSUpdateFinishedSuccessfully() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getById(1L)).thenReturn(stackDto);

        underTest.accept(new Event<>(new UpdatePublicDnsEntriesInPemRequest(1L)));

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(stringArgumentCaptor.capture(), eventArgumentCaptor.capture());
        verify(clusterPublicEndpointManagementService, times(1)).refreshDnsEntries(stackDto);

        assertEquals(UpdatePublicDnsEntriesFlowEvent.UPDATE_PUBLIC_DNS_ENTRIES_SUCCEEDED_EVENT.event(), stringArgumentCaptor.getValue());
        assertEquals(UpdatePublicDnsEntriesInPemFinished.class, eventArgumentCaptor.getValue().getData().getClass());
    }
}