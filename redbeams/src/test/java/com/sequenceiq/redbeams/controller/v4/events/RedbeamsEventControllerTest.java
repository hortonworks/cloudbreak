package com.sequenceiq.redbeams.controller.v4.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.redbeams.service.events.RedbeamsEventsService;

@ExtendWith(MockitoExtension.class)
class RedbeamsEventControllerTest {

    private static final String RESOURCE_CRN = "crn:cdp:redbeams:us-west-1:acc:databaseServer:res";

    @Mock
    private RedbeamsEventsService redbeamsEventsService;

    @InjectMocks
    private RedbeamsEventController underTest;

    @Test
    void getAuditEventsDelegatesToService() {
        List<StructuredEventType> types = List.of(StructuredEventType.NOTIFICATION);
        List<CDPStructuredEvent> events = List.of(new CDPStructuredNotificationEvent());
        when(redbeamsEventsService.getPagedAuditEvents(RESOURCE_CRN, types, 1, 25)).thenReturn(events);

        List<CDPStructuredEvent> result = underTest.getAuditEvents(RESOURCE_CRN, types, 1, 25);

        assertThat(result).isSameAs(events);
    }
}
