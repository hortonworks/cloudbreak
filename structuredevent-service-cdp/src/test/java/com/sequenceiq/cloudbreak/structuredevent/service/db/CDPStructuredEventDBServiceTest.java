package com.sequenceiq.cloudbreak.structuredevent.service.db;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.FLOW;
import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.NOTIFICATION;
import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.REST;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sequenceiq.cloudbreak.structuredevent.repository.CDPPagingStructuredEventRepository;

@ExtendWith(MockitoExtension.class)
public class CDPStructuredEventDBServiceTest {

    @InjectMocks
    private CDPStructuredEventDBService underTest;

    @Mock
    private CDPPagingStructuredEventRepository pagingStructuredEventRepository;

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
}
