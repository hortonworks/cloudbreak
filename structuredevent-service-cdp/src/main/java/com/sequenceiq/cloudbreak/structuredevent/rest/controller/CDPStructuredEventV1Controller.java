package com.sequenceiq.cloudbreak.structuredevent.rest.controller;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CustomPermissionCheck;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.utils.EventAuthorizationDto;
import com.sequenceiq.authorization.utils.EventAuthorizationUtils;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventV1Endpoint;
import com.sequenceiq.cloudbreak.structuredevent.service.db.CDPStructuredEventDBService;

@Controller
public class CDPStructuredEventV1Controller implements CDPStructuredEventV1Endpoint {

    @Inject
    private CDPStructuredEventDBService structuredEventDBService;

    @Inject
    private EventAuthorizationUtils eventAuthorizationUtils;

    @Override
    @CustomPermissionCheck
    public List<CDPStructuredEvent> getAuditEvents(@ResourceCrn String resourceCrn, List<StructuredEventType> types, Integer page, Integer size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        List<CDPStructuredEvent> events = structuredEventDBService.getPagedEventsOfResource(types, resourceCrn, pageable).getContent();
        if (events.isEmpty()) {
            return Collections.emptyList();
        }
        eventAuthorizationUtils.checkPermissionBasedOnResourceTypeAndCrn(collectDtosFromEvents(events));
        return events;
    }

    @Override
    @CustomPermissionCheck
    public Response getAuditEventsZip(@ResourceCrn String resourceCrn, List<StructuredEventType> types) {
        Collection<CDPStructuredNotificationEvent> events = structuredEventDBService.getEventsOfResource(types, resourceCrn);
        if (events.isEmpty()) {
            return Response.noContent().build();
        }
        eventAuthorizationUtils.checkPermissionBasedOnResourceTypeAndCrn(collectDtosFromEvents(events));
        return getAuditEventsZipResponse(events, resourceCrn);
    }

    private Response getAuditEventsZipResponse(Collection<CDPStructuredNotificationEvent> events, String resourceCrn) {
        StreamingOutput streamingOutput = output -> {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(output)) {
                zipOutputStream.putNextEntry(new ZipEntry("struct-events.json"));
                zipOutputStream.write(JsonUtil.writeValueAsString(events).getBytes());
                zipOutputStream.closeEntry();
            }
        };
        String resourceType = Crn.safeFromString(resourceCrn).getResourceType().getName();
        String fileName = String.format("audit-%s.zip", resourceType);
        return Response.ok(streamingOutput).header("content-disposition", String.format("attachment; filename = %s", fileName)).build();
    }

    private Set<EventAuthorizationDto> collectDtosFromEvents(Collection<? extends CDPStructuredEvent> events) {
        return events.stream().map(event -> event.getOperation())
                .map(details -> new EventAuthorizationDto(details.getResourceCrn(), details.getResourceType()))
                .collect(Collectors.toSet());
    }

}
