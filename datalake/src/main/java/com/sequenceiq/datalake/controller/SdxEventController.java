package com.sequenceiq.datalake.controller;

import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CustomPermissionCheck;
import com.sequenceiq.authorization.utils.EventAuthorizationDto;
import com.sequenceiq.authorization.utils.EventAuthorizationUtils;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.db.CDPStructuredEventDBService;
import com.sequenceiq.sdx.api.endpoint.SdxEventEndpoint;

@Controller
public class SdxEventController implements SdxEventEndpoint {

    public static final int DEFAULT_PAGE_SIZE = 100;

    public static final int DEFAULT_STARTING_PAGE = 0;

    @Inject
    private CDPStructuredEventDBService cdpStructuredEventDBService;

    @Inject
    private EventAuthorizationUtils eventAuthorizationUtils;

    /**
     * Retrieves audit events for the provided Data Lake CRN.
     *
     * @param resourceCrn a Data Lake CRN
     * @param types       types of structured events to retrieve
     * @return structured events for the provided Data Lake CRN
     */
    @Override
    @CustomPermissionCheck
    public List<CDPStructuredEvent> getAuditEvents(String resourceCrn, List<StructuredEventType> types) {
        PageRequest pageable = PageRequest.of(DEFAULT_STARTING_PAGE, DEFAULT_PAGE_SIZE, Sort.by("timestamp").descending());

        List<CDPStructuredEvent> dlEvents = cdpStructuredEventDBService.getPagedEventsOfResource(types, resourceCrn, pageable).getContent();

        if (dlEvents.isEmpty()) {
            return Collections.emptyList();
        }

        // TODO Pull the events from CB service and merge them.

        // custom permission check
        eventAuthorizationUtils.checkPermissionBasedOnResourceTypeAndCrn(collectDtosFromEvents(dlEvents));

        return dlEvents;
    }

    /**
     * Converts a collection of {@code CDPStructuredEvent}s to simple objects containing Resource CRN and Type.
     * <p>
     * This method essentially does a projection, where the collection of events is mapped to a set of objects that contain fewer properties.
     *
     * @param events collection of structured events to map
     * @return set of derived resource CRNs and Types associated with provided events.
     */
    private Set<EventAuthorizationDto> collectDtosFromEvents(Collection<? extends CDPStructuredEvent> events) {
        return events.stream().map(CDPStructuredEvent::getOperation)
                .map(details -> new EventAuthorizationDto(details.getResourceCrn(), details.getResourceType()))
                .collect(toSet());
    }
}
