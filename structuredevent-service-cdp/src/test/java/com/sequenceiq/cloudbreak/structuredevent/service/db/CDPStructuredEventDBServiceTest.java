package com.sequenceiq.cloudbreak.structuredevent.service.db;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.FLOW;
import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.NOTIFICATION;
import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.REST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.structuredevent.domain.CDPStructuredEventEntity;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.repository.CDPPagingStructuredEventRepository;
import com.sequenceiq.cloudbreak.structuredevent.repository.CDPStructuredEventRepository;
import com.sequenceiq.cloudbreak.structuredevent.service.converter.CDPStructuredEventEntityToCDPStructuredEventConverter;
import com.sequenceiq.cloudbreak.structuredevent.service.converter.CDPStructuredEventToCDPStructuredEventEntityConverter;

@ExtendWith(MockitoExtension.class)
public class CDPStructuredEventDBServiceTest {

    private static final String RESOURCE_CRN = "crn:cdp:datalake:us-west-1:hortonworks:datalake:12345";

    private static final String RESOURCE_CRN_2 = "crn:cdp:datalake:us-west-1:hortonworks:datalake:67890";

    @InjectMocks
    private CDPStructuredEventDBService underTest;

    @Mock
    private CDPPagingStructuredEventRepository pagingStructuredEventRepository;

    @Mock
    private CDPStructuredEventRepository structuredEventRepository;

    @Mock
    private CDPStructuredEventToCDPStructuredEventEntityConverter cdpStructuredEventToCDPStructuredEventEntityConverter;

    @Mock
    private CDPStructuredEventEntityToCDPStructuredEventConverter cdpStructuredEventEntityToCDPStructuredEventConverter;

    @Test
    public void testGetPagedEventsOfResourceWhenEventTypesEmpty() {
        ArgumentCaptor<List> eventTypes = ArgumentCaptor.forClass(List.class);
        Pageable unpaged = Pageable.unpaged();

        when(pagingStructuredEventRepository.findByEventTypeInAndResourceCrn(eventTypes.capture(), eq(RESOURCE_CRN), eq(unpaged)))
                .thenReturn(new SliceImpl<>(Collections.emptyList()));
        underTest.getPagedEventsOfResource(Collections.emptyList(), RESOURCE_CRN, unpaged);

        List actual = eventTypes.getValue();
        assertEquals(List.of(NOTIFICATION, REST, FLOW), actual);
    }

    @Test
    public void testGetPagedEventsOfResourceWhenEventTypesNotEmpty() {
        ArgumentCaptor<List> eventTypes = ArgumentCaptor.forClass(List.class);
        Pageable unpaged = Pageable.unpaged();

        when(pagingStructuredEventRepository.findByEventTypeInAndResourceCrn(eventTypes.capture(), eq(RESOURCE_CRN), eq(unpaged)))
                .thenReturn(new SliceImpl<>(Collections.emptyList()));
        underTest.getPagedEventsOfResource(List.of(NOTIFICATION), RESOURCE_CRN, unpaged);

        List actual = eventTypes.getValue();
        assertEquals(List.of(NOTIFICATION), actual);
    }

    @Test
    public void testGetPagedEventsOfResourceReturnsConvertedEvents() {
        Pageable pageable = PageRequest.of(0, 10);
        CDPStructuredEventEntity entity1 = new CDPStructuredEventEntity();
        CDPStructuredEventEntity entity2 = new CDPStructuredEventEntity();
        CDPStructuredNotificationEvent converted1 = new CDPStructuredNotificationEvent();
        CDPStructuredNotificationEvent converted2 = new CDPStructuredNotificationEvent();

        when(pagingStructuredEventRepository.findByEventTypeInAndResourceCrn(any(), eq(RESOURCE_CRN), eq(pageable)))
                .thenReturn(new SliceImpl<>(List.of(entity1, entity2)));
        when(cdpStructuredEventEntityToCDPStructuredEventConverter.convert(entity1)).thenReturn(converted1);
        when(cdpStructuredEventEntityToCDPStructuredEventConverter.convert(entity2)).thenReturn(converted2);

        Page<CDPStructuredEvent> result = underTest.getPagedEventsOfResource(List.of(NOTIFICATION), RESOURCE_CRN, pageable);

        assertEquals(2, result.getContent().size());
        assertEquals(converted1, result.getContent().get(0));
        assertEquals(converted2, result.getContent().get(1));
    }

    @Test
    public void testGetPagedEventsOfResourceWhenRepositoryReturnsNull() {
        Pageable pageable = PageRequest.of(0, 10);

        when(pagingStructuredEventRepository.findByEventTypeInAndResourceCrn(any(), eq(RESOURCE_CRN), eq(pageable)))
                .thenReturn(null);

        Page<CDPStructuredEvent> result = underTest.getPagedEventsOfResource(List.of(NOTIFICATION), RESOURCE_CRN, pageable);

        assertTrue(result.getContent().isEmpty());
    }

    @Test
    public void testGetPagedEventsOfResourceThrowsCloudbreakServiceExceptionOnError() {
        Pageable pageable = PageRequest.of(0, 10);

        when(pagingStructuredEventRepository.findByEventTypeInAndResourceCrn(any(), eq(RESOURCE_CRN), eq(pageable)))
                .thenThrow(new RuntimeException("DB timeout"));

        assertThrows(CloudbreakServiceException.class,
                () -> underTest.getPagedEventsOfResource(List.of(NOTIFICATION), RESOURCE_CRN, pageable));
    }

    @Test
    public void testGetPagedEventsOfResourcesReturnsConvertedEvents() {
        Pageable pageable = PageRequest.of(0, 10);
        List<String> crns = List.of(RESOURCE_CRN, RESOURCE_CRN_2);
        CDPStructuredEventEntity entity = new CDPStructuredEventEntity();
        CDPStructuredNotificationEvent converted = new CDPStructuredNotificationEvent();

        when(pagingStructuredEventRepository.findByEventTypeInAndResourceCrnIn(any(), eq(crns), eq(pageable)))
                .thenReturn(new SliceImpl<>(List.of(entity)));
        when(cdpStructuredEventEntityToCDPStructuredEventConverter.convert(entity)).thenReturn(converted);

        Page<CDPStructuredEvent> result = underTest.getPagedEventsOfResources(List.of(NOTIFICATION), crns, pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(converted, result.getContent().get(0));
    }

    @Test
    public void testGetPagedEventsOfResourcesWhenEventTypesEmpty() {
        ArgumentCaptor<List> eventTypes = ArgumentCaptor.forClass(List.class);
        Pageable pageable = PageRequest.of(0, 10);
        List<String> crns = List.of(RESOURCE_CRN);

        when(pagingStructuredEventRepository.findByEventTypeInAndResourceCrnIn(eventTypes.capture(), eq(crns), eq(pageable)))
                .thenReturn(new SliceImpl<>(Collections.emptyList()));
        underTest.getPagedEventsOfResources(Collections.emptyList(), crns, pageable);

        assertEquals(List.of(NOTIFICATION, REST, FLOW), eventTypes.getValue());
    }

    @Test
    public void testGetPagedEventsOfResourcesWhenRepositoryReturnsNull() {
        Pageable pageable = PageRequest.of(0, 10);
        List<String> crns = List.of(RESOURCE_CRN);

        when(pagingStructuredEventRepository.findByEventTypeInAndResourceCrnIn(any(), eq(crns), eq(pageable)))
                .thenReturn(null);

        Page<CDPStructuredEvent> result = underTest.getPagedEventsOfResources(List.of(NOTIFICATION), crns, pageable);

        assertTrue(result.getContent().isEmpty());
    }

    @Test
    public void testGetPagedEventsOfResourcesThrowsCloudbreakServiceExceptionOnError() {
        Pageable pageable = PageRequest.of(0, 10);
        List<String> crns = List.of(RESOURCE_CRN);

        when(pagingStructuredEventRepository.findByEventTypeInAndResourceCrnIn(any(), eq(crns), eq(pageable)))
                .thenThrow(new RuntimeException("DB timeout"));

        assertThrows(CloudbreakServiceException.class,
                () -> underTest.getPagedEventsOfResources(List.of(NOTIFICATION), crns, pageable));
    }

    @Test
    public void testGetEventsOfResourceReturnsConvertedEvents() {
        ReflectionTestUtils.setField(underTest, "maxSize", 20000);
        CDPStructuredEventEntity entity = new CDPStructuredEventEntity();
        CDPStructuredNotificationEvent converted = new CDPStructuredNotificationEvent();
        PageRequest expectedPageRequest = PageRequest.of(0, 20000, Sort.by("timestamp").descending());

        when(pagingStructuredEventRepository.findByEventTypeInAndResourceCrn(any(), eq(RESOURCE_CRN), eq(expectedPageRequest)))
                .thenReturn(new SliceImpl<>(List.of(entity)));
        when(cdpStructuredEventEntityToCDPStructuredEventConverter.convert(entity)).thenReturn(converted);

        List<CDPStructuredEvent> result = underTest.getEventsOfResource(List.of(NOTIFICATION), RESOURCE_CRN);

        assertEquals(1, result.size());
        assertEquals(converted, result.get(0));
    }

    @Test
    public void testGetEventsOfResourceWhenRepositoryReturnsNull() {
        ReflectionTestUtils.setField(underTest, "maxSize", 20000);

        when(pagingStructuredEventRepository.findByEventTypeInAndResourceCrn(any(), eq(RESOURCE_CRN), any()))
                .thenReturn(null);

        List<CDPStructuredEvent> result = underTest.getEventsOfResource(List.of(NOTIFICATION), RESOURCE_CRN);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetEventsOfResourceThrowsCloudbreakServiceExceptionOnError() {
        ReflectionTestUtils.setField(underTest, "maxSize", 20000);

        when(pagingStructuredEventRepository.findByEventTypeInAndResourceCrn(any(), eq(RESOURCE_CRN), any()))
                .thenThrow(new RuntimeException("DB timeout"));

        assertThrows(CloudbreakServiceException.class,
                () -> underTest.getEventsOfResource(List.of(NOTIFICATION), RESOURCE_CRN));
    }

    @Test
    public void testGetEventsOfResourcesReturnsConvertedEvents() {
        ReflectionTestUtils.setField(underTest, "maxSize", 20000);
        List<String> crns = List.of(RESOURCE_CRN, RESOURCE_CRN_2);
        CDPStructuredEventEntity entity = new CDPStructuredEventEntity();
        CDPStructuredNotificationEvent converted = new CDPStructuredNotificationEvent();
        PageRequest expectedPageRequest = PageRequest.of(0, 20000, Sort.by("timestamp").descending());

        when(pagingStructuredEventRepository.findByEventTypeInAndResourceCrnIn(any(), eq(crns), eq(expectedPageRequest)))
                .thenReturn(new SliceImpl<>(List.of(entity)));
        when(cdpStructuredEventEntityToCDPStructuredEventConverter.convert(entity)).thenReturn(converted);

        List<CDPStructuredEvent> result = underTest.getEventsOfResources(List.of(NOTIFICATION), crns);

        assertEquals(1, result.size());
        assertEquals(converted, result.get(0));
    }

    @Test
    public void testGetEventsOfResourcesWhenRepositoryReturnsNull() {
        ReflectionTestUtils.setField(underTest, "maxSize", 20000);
        List<String> crns = List.of(RESOURCE_CRN);

        when(pagingStructuredEventRepository.findByEventTypeInAndResourceCrnIn(any(), eq(crns), any()))
                .thenReturn(null);

        List<CDPStructuredEvent> result = underTest.getEventsOfResources(List.of(NOTIFICATION), crns);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetEventsOfResourcesThrowsCloudbreakServiceExceptionOnError() {
        ReflectionTestUtils.setField(underTest, "maxSize", 20000);
        List<String> crns = List.of(RESOURCE_CRN);

        when(pagingStructuredEventRepository.findByEventTypeInAndResourceCrnIn(any(), eq(crns), any()))
                .thenThrow(new RuntimeException("DB timeout"));

        assertThrows(CloudbreakServiceException.class,
                () -> underTest.getEventsOfResources(List.of(NOTIFICATION), crns));
    }

    @Test
    public void testCreateWhenResourceCrnIsNull() {
        CDPStructuredEvent event = new CDPStructuredRestCallEvent();
        CDPOperationDetails operation = new CDPOperationDetails();
        operation.setResourceCrn(null);
        event.setOperation(operation);
        underTest.create(event);
        verify(cdpStructuredEventToCDPStructuredEventEntityConverter, never()).convert(event);
    }

    @Test
    public void testCreateWhenResourceCrnIsEmpty() {
        CDPStructuredEvent event = new CDPStructuredRestCallEvent();
        CDPOperationDetails operation = new CDPOperationDetails();
        operation.setResourceCrn("");
        event.setOperation(operation);
        underTest.create(event);
        verify(cdpStructuredEventToCDPStructuredEventEntityConverter, never()).convert(event);
    }

    @Test
    public void testCreateWhenResourceCrnIsNotEmpty() {
        CDPStructuredEvent event = new CDPStructuredRestCallEvent();
        event.setType(CDPStructuredNotificationEvent.class.getSimpleName());
        CDPOperationDetails operation = new CDPOperationDetails();
        operation.setResourceCrn("crn:cdp:cloudbreak:us-west-1:someone:stack:12345");
        event.setOperation(operation);
        CDPStructuredEventEntity entity = new CDPStructuredEventEntity();
        when(cdpStructuredEventToCDPStructuredEventEntityConverter.convert(event)).thenReturn(entity);

        underTest.create(event);

        assertEquals("CDPStructuredNotificationEvent", event.getType());
        verify(cdpStructuredEventToCDPStructuredEventEntityConverter, times(1)).convert(event);
        verify(structuredEventRepository, times(1)).save(entity);
    }

    @Test
    public void testCreateWithRestEvent() {
        CDPStructuredEvent event = getCdpStructuredEvent(CDPStructuredRestCallEvent.class.getSimpleName());

        underTest.create(event);

        assertEquals("CDPStructuredRestCallEvent", event.getType());
        verify(structuredEventRepository, times(0)).save(any());
    }

    @Test
    public void testCreateWithFlowEvent() {
        CDPStructuredEvent event = getCdpStructuredEvent(CDPStructuredFlowEvent.class.getSimpleName());

        underTest.create(event);

        assertEquals("CDPStructuredFlowEvent", event.getType());
        verify(structuredEventRepository, times(0)).save(any());
    }

    private CDPStructuredEvent getCdpStructuredEvent(String simpleName) {
        CDPStructuredEvent event = new CDPStructuredRestCallEvent();
        event.setType(simpleName);
        CDPOperationDetails operation = new CDPOperationDetails();
        operation.setResourceCrn("crn");
        event.setOperation(operation);
        return event;
    }
}
