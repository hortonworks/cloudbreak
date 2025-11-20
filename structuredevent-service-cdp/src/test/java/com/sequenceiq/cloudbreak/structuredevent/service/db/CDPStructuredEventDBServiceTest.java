package com.sequenceiq.cloudbreak.structuredevent.service.db;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.FLOW;
import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.NOTIFICATION;
import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.REST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

        when(pagingStructuredEventRepository.findByEventTypeInAndResourceCrn(eventTypes.capture(), eq("crn"), eq(unpaged))).thenReturn(Page.empty());
        underTest.getPagedEventsOfResource(Collections.emptyList(), "crn", unpaged);

        List actual = eventTypes.getValue();
        assertEquals(List.of(NOTIFICATION, REST, FLOW), actual);
    }

    @Test
    public void testGetPagedEventsOfResourceWhenEventTypesNotEmpty() {
        ArgumentCaptor<List> eventTypes = ArgumentCaptor.forClass(List.class);
        Pageable unpaged = Pageable.unpaged();

        when(pagingStructuredEventRepository.findByEventTypeInAndResourceCrn(eventTypes.capture(), eq("crn"), eq(unpaged))).thenReturn(Page.empty());
        underTest.getPagedEventsOfResource(List.of(NOTIFICATION), "crn", unpaged);

        List actual = eventTypes.getValue();
        assertEquals(List.of(NOTIFICATION), actual);
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
        verify(cdpStructuredEventToCDPStructuredEventEntityConverter, Mockito.times(1)).convert(event);
        verify(structuredEventRepository, Mockito.times(1)).save(entity);
    }

    @Test
    public void testCreateWithRestEvent() {
        CDPStructuredEvent event = getCdpStructuredEvent(CDPStructuredRestCallEvent.class.getSimpleName());

        underTest.create(event);

        assertEquals("CDPStructuredRestCallEvent", event.getType());
        verify(structuredEventRepository, Mockito.times(0)).save(any());
    }

    @Test
    public void testCreateWithFlowEvent() {
        CDPStructuredEvent event = getCdpStructuredEvent(CDPStructuredFlowEvent.class.getSimpleName());

        underTest.create(event);

        assertEquals("CDPStructuredFlowEvent", event.getType());
        verify(structuredEventRepository, Mockito.times(0)).save(any());
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
