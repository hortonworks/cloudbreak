package com.sequenceiq.cloudbreak.structuredevent.rest.controller;

import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPAuditEventV1Endpoint;
import com.sequenceiq.cloudbreak.structuredevent.service.db.CDPStructuredEventDBService;

@Controller
public class CDPAuditEventV1Controller implements CDPAuditEventV1Endpoint {

    @Inject
    private CDPStructuredEventDBService structuredEventDBService;

    @Override
    public Page<CDPStructuredNotificationEvent> getAuditEvents(String resourceCrn, Integer page, Integer size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return structuredEventDBService.getPagedNotificationEventsOfResource(StructuredEventType.NOTIFICATION, resourceCrn, pageable);
    }

    @Override
    public Response getAuditEventsZip(String resourceCrn) {
        Page<CDPStructuredNotificationEvent> auditEvents = getAuditEvents(resourceCrn, 0, 1000);
        return getAuditEventsZipResponse(auditEvents.getContent(), resourceCrn);
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
}
