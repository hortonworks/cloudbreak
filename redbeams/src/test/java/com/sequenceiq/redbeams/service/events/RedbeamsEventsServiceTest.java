package com.sequenceiq.redbeams.service.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.db.CDPStructuredEventDBService;

@ExtendWith(MockitoExtension.class)
class RedbeamsEventsServiceTest {

    private static final String RESOURCE_CRN = "crn:cdp:redbeams:us-west-1:acc:databaseServer:res";

    @Mock
    private CDPStructuredEventDBService cdpStructuredEventDBService;

    @InjectMocks
    private RedbeamsEventsService underTest;

    @Test
    void getPagedAuditEventsRequestsTimestampDescendingPageAndReturnsContent() {
        List<StructuredEventType> types = List.of(StructuredEventType.NOTIFICATION);
        CDPStructuredEvent event = new CDPStructuredNotificationEvent();
        PageRequest expectedPageable = PageRequest.of(2, 50, Sort.by("timestamp").descending());
        when(cdpStructuredEventDBService.getPagedEventsOfResource(eq(types), eq(RESOURCE_CRN), eq(expectedPageable)))
                .thenReturn(new PageImpl<>(List.of(event)));

        List<CDPStructuredEvent> result = underTest.getPagedAuditEvents(RESOURCE_CRN, types, 2, 50);

        assertThat(result).containsExactly(event);
    }

    @Test
    void getPagedAuditEventsPassesNullTypesThroughWithDefaultPage() {
        PageRequest expectedPageable = PageRequest.of(0, 100, Sort.by("timestamp").descending());
        when(cdpStructuredEventDBService.getPagedEventsOfResource(eq(null), eq(RESOURCE_CRN), eq(expectedPageable)))
                .thenReturn(new PageImpl<>(List.of()));

        List<CDPStructuredEvent> result = underTest.getPagedAuditEvents(RESOURCE_CRN, null, 0, 100);

        assertThat(result).isEmpty();
    }
}
