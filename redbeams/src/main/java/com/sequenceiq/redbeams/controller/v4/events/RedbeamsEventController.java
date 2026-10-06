package com.sequenceiq.redbeams.controller.v4.events;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.redbeams.api.endpoint.v4.events.RedbeamsEventV4Endpoint;
import com.sequenceiq.redbeams.service.events.RedbeamsEventsService;

@Controller
@InternalOnly
public class RedbeamsEventController implements RedbeamsEventV4Endpoint {

    @Inject
    private RedbeamsEventsService redbeamsEventsService;

    /**
     * Retrieves the CDP structured events redbeams stored for the given database server resource CRN.
     *
     * @param resourceCrn the database server resource CRN
     * @param types       types of structured events to retrieve (all types when {@code null})
     * @return structured events for the resource, newest first
     */
    @Override
    @InternalOnly
    public List<CDPStructuredEvent> getAuditEvents(@ResourceCrn String resourceCrn, List<StructuredEventType> types, Integer page, Integer size) {
        return redbeamsEventsService.getPagedAuditEvents(resourceCrn, types, page, size);
    }
}
