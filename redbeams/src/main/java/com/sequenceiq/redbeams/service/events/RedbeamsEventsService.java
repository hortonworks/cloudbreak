package com.sequenceiq.redbeams.service.events;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.db.CDPStructuredEventDBService;

/**
 * Reads the CDP structured events redbeams persisted for a database server resource (keyed by its resource CRN),
 * newest first. Backs {@code RedbeamsEventV4Endpoint}, which is called internally (e.g. by core assembling a stack's event feed).
 */
@Service
public class RedbeamsEventsService {

    @Inject
    private CDPStructuredEventDBService cdpStructuredEventDBService;

    public List<CDPStructuredEvent> getPagedAuditEvents(String resourceCrn, List<StructuredEventType> types, Integer page, Integer size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return cdpStructuredEventDBService.getPagedEventsOfResource(types, resourceCrn, pageable).getContent();
    }
}
