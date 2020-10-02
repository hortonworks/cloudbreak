package com.sequenceiq.cloudbreak.structuredevent.service.db;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.FLOW;
import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.NOTIFICATION;
import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.REST;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sequenceiq.cloudbreak.structuredevent.domain.CDPStructuredEventEntity;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.repository.CDPPagingStructuredEventRepository;
import com.sequenceiq.cloudbreak.structuredevent.repository.CDPStructuredEventRepository;

@ExtendWith(MockitoExtension.class)
public class CDPStructuredEventDBServiceTest {

    @InjectMocks
    private CDPStructuredEventDBService underTest;

    @Mock
    private CDPPagingStructuredEventRepository pagingStructuredEventRepository;

    @Mock
    private ConversionService conversionService;

    @Mock
    private CDPStructuredEventRepository structuredEventRepository;

    @Test
    public void testGetPagedEventsOfResourceWhenEventTypesEmpty() {
        ArgumentCaptor<List> eventTypes = ArgumentCaptor.forClass(List.class);
        Pageable unpaged = Pageable.unpaged();

        when(pagingStructuredEventRepository.findByEventTypeInAndResourceCrn(eventTypes.capture(), eq("crn"), eq(unpaged))).thenReturn(Page.empty());
        underTest.getPagedEventsOfResource(Collections.emptyList(), "crn", unpaged);

        List actual = eventTypes.getValue();
        Assertions.assertEquals(List.of(NOTIFICATION, REST, FLOW), actual);
    }

    @Test
    public void testGetPagedEventsOfResourceWhenEventTypesNotEmpty() {
        ArgumentCaptor<List> eventTypes = ArgumentCaptor.forClass(List.class);
        Pageable unpaged = Pageable.unpaged();

        when(pagingStructuredEventRepository.findByEventTypeInAndResourceCrn(eventTypes.capture(), eq("crn"), eq(unpaged))).thenReturn(Page.empty());
        underTest.getPagedEventsOfResource(List.of(NOTIFICATION), "crn", unpaged);

        List actual = eventTypes.getValue();
        Assertions.assertEquals(List.of(NOTIFICATION), actual);
    }

    @Test
    public void testCreateWhenResourceCrnIsNull() {
        CDPStructuredEvent event = new CDPStructuredRestCallEvent();
        CDPOperationDetails operation = new CDPOperationDetails();
        operation.setResourceCrn(null);
        event.setOperation(operation);
        underTest.create(event);
        verify(conversionService, never()).convert(event, CDPStructuredEventEntity.class);
    }

    @Test
    public void testCreateWhenResourceCrnIsEmpty() {
        CDPStructuredEvent event = new CDPStructuredRestCallEvent();
        CDPOperationDetails operation = new CDPOperationDetails();
        operation.setResourceCrn("");
        event.setOperation(operation);
        underTest.create(event);
        verify(conversionService, never()).convert(event, CDPStructuredEventEntity.class);
    }

    @Test
    public void testCreateWhenResourceCrnIsNotEmpty() {
        CDPStructuredEvent event = new CDPStructuredRestCallEvent();
        CDPOperationDetails operation = new CDPOperationDetails();
        operation.setResourceCrn("crn");
        event.setOperation(operation);
        CDPStructuredEventEntity entity = new CDPStructuredEventEntity();
        when(conversionService.convert(event, CDPStructuredEventEntity.class)).thenReturn(entity);
        underTest.create(event);
        verify(conversionService, Mockito.times(1)).convert(event, CDPStructuredEventEntity.class);
        verify(structuredEventRepository, Mockito.times(1)).save(entity);
    }
}
