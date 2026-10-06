package com.sequenceiq.cloudbreak.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsClientService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.structuredevent.converter.CdpStructuredEventToCloudbreakEventV4ResponseConverter;
import com.sequenceiq.cloudbreak.structuredevent.converter.StructuredNotificationEventToCloudbreakEventV4ResponseConverter;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.cloudbreak.view.ClusterView;

@ExtendWith(MockitoExtension.class)
class DefaultCloudbreakEventsFacadeTest {

    private static final Long STACK_ID = 1L;

    private static final String DB_SERVER_CRN = "crn:cdp:redbeams:us-west-1:acc:databaseServer:res";

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @Mock
    private StructuredNotificationEventToCloudbreakEventV4ResponseConverter eventConverter;

    @Mock
    private CdpStructuredEventToCloudbreakEventV4ResponseConverter cdpEventConverter;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private RedbeamsClientService redbeamsClientService;

    @InjectMocks
    private DefaultCloudbreakEventsFacade underTest;

    @Test
    void retrieveEventsByStackReturnsOnlyCoreEventsWhenNoExternalDatabase() {
        StructuredNotificationEvent coreEv1 = mockCoreEvent(100L);
        StructuredNotificationEvent coreEv2 = mockCoreEvent(300L);
        when(cloudbreakEventService.cloudbreakEventsForStack(eq(STACK_ID), anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(coreEv1, coreEv2)));
        ClusterView cluster = mock(ClusterView.class);
        when(cluster.hasExternalDatabase()).thenReturn(false);
        when(stackDtoService.getClusterViewByStackId(STACK_ID)).thenReturn(cluster);

        Page<CloudbreakEventV4Response> result = underTest.retrieveEventsByStack(STACK_ID, StackType.WORKLOAD, Pageable.unpaged());

        assertThat(result.getContent()).extracting(CloudbreakEventV4Response::getEventTimestamp).containsExactly(300L, 100L);
        verify(redbeamsClientService, never()).getStructuredEventsByCrn(anyString());
    }

    @Test
    void retrieveEventsByStackMergesSortsAndPagesWhenExternalDatabasePresent() {
        StructuredNotificationEvent coreEv1 = mockCoreEvent(100L);
        StructuredNotificationEvent coreEv2 = mockCoreEvent(300L);
        when(cloudbreakEventService.cloudbreakEventsForStack(eq(STACK_ID), anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(coreEv1, coreEv2)));
        ClusterView cluster = mock(ClusterView.class);
        when(cluster.hasExternalDatabase()).thenReturn(true);
        when(cluster.getDatabaseServerCrn()).thenReturn(DB_SERVER_CRN);
        when(stackDtoService.getClusterViewByStackId(STACK_ID)).thenReturn(cluster);
        CDPStructuredEvent rbEv1 = mockRedbeamsEvent(200L);
        CDPStructuredEvent rbEv2 = mockRedbeamsEvent(400L);
        when(redbeamsClientService.getStructuredEventsByCrn(DB_SERVER_CRN)).thenReturn(List.of(rbEv1, rbEv2));

        // first page of size 2 of the combined, timestamp-descending list [400, 300, 200, 100]
        Page<CloudbreakEventV4Response> result = underTest.retrieveEventsByStack(STACK_ID, StackType.WORKLOAD, PageRequest.of(0, 2));

        assertThat(result.getTotalElements()).isEqualTo(4L);
        assertThat(result.getContent()).extracting(CloudbreakEventV4Response::getEventTimestamp).containsExactly(400L, 300L);
    }

    @Test
    void retrieveEventsByStackReturnsSecondPageSlice() {
        StructuredNotificationEvent coreEv1 = mockCoreEvent(100L);
        StructuredNotificationEvent coreEv2 = mockCoreEvent(300L);
        when(cloudbreakEventService.cloudbreakEventsForStack(eq(STACK_ID), anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(coreEv1, coreEv2)));
        ClusterView cluster = mock(ClusterView.class);
        when(cluster.hasExternalDatabase()).thenReturn(true);
        when(cluster.getDatabaseServerCrn()).thenReturn(DB_SERVER_CRN);
        when(stackDtoService.getClusterViewByStackId(STACK_ID)).thenReturn(cluster);
        CDPStructuredEvent rbEv1 = mockRedbeamsEvent(200L);
        CDPStructuredEvent rbEv2 = mockRedbeamsEvent(400L);
        when(redbeamsClientService.getStructuredEventsByCrn(DB_SERVER_CRN)).thenReturn(List.of(rbEv1, rbEv2));

        Page<CloudbreakEventV4Response> result = underTest.retrieveEventsByStack(STACK_ID, StackType.WORKLOAD, PageRequest.of(1, 2));

        assertThat(result.getContent()).extracting(CloudbreakEventV4Response::getEventTimestamp).containsExactly(200L, 100L);
    }

    @Test
    void retrieveEventsByStackFallsBackToCoreOnlyWhenRedbeamsFails() {
        StructuredNotificationEvent coreEv1 = mockCoreEvent(100L);
        when(cloudbreakEventService.cloudbreakEventsForStack(eq(STACK_ID), anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(coreEv1)));
        ClusterView cluster = mock(ClusterView.class);
        when(cluster.hasExternalDatabase()).thenReturn(true);
        when(cluster.getDatabaseServerCrn()).thenReturn(DB_SERVER_CRN);
        when(stackDtoService.getClusterViewByStackId(STACK_ID)).thenReturn(cluster);
        when(redbeamsClientService.getStructuredEventsByCrn(DB_SERVER_CRN)).thenThrow(new RuntimeException("boom"));

        Page<CloudbreakEventV4Response> result = underTest.retrieveEventsByStack(STACK_ID, StackType.WORKLOAD, Pageable.unpaged());

        assertThat(result.getContent()).extracting(CloudbreakEventV4Response::getEventTimestamp).containsExactly(100L);
    }

    @Test
    void retrieveEventsByStackHandlesNullClusterView() {
        StructuredNotificationEvent coreEv1 = mockCoreEvent(100L);
        when(cloudbreakEventService.cloudbreakEventsForStack(eq(STACK_ID), anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(coreEv1)));
        when(stackDtoService.getClusterViewByStackId(STACK_ID)).thenReturn(null);

        Page<CloudbreakEventV4Response> result = underTest.retrieveEventsByStack(STACK_ID, StackType.WORKLOAD, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        verify(redbeamsClientService, never()).getStructuredEventsByCrn(anyString());
    }

    private StructuredNotificationEvent mockCoreEvent(long timestamp) {
        StructuredNotificationEvent event = mock(StructuredNotificationEvent.class);
        lenient().when(eventConverter.convert(event)).thenReturn(response(timestamp));
        return event;
    }

    private CDPStructuredEvent mockRedbeamsEvent(long timestamp) {
        CDPStructuredNotificationEvent event = new CDPStructuredNotificationEvent();
        lenient().when(cdpEventConverter.convert(event)).thenReturn(response(timestamp));
        return event;
    }

    private static CloudbreakEventV4Response response(long timestamp) {
        CloudbreakEventV4Response response = new CloudbreakEventV4Response();
        response.setEventTimestamp(timestamp);
        return response;
    }

    private static <T> T mock(Class<T> type) {
        return org.mockito.Mockito.mock(type);
    }
}
