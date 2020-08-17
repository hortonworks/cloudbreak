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

import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPAuditEventV1Endpoint;
import com.sequenceiq.cloudbreak.structuredevent.rest.model.CDPAuditEventV4Response;
import com.sequenceiq.cloudbreak.structuredevent.rest.model.CDPAuditEventV4Responses;
import com.sequenceiq.cloudbreak.structuredevent.service.CDPStructuredEventDBService;

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
        CDPAuditEventV4Responses auditEvents = getAuditEvents(resourceCrn);
        return getAuditEventsZipResponse(auditEvents.getResponses());
    }

    private Response getAuditEventsZipResponse(Collection<CDPAuditEventV4Response> auditEventV4Responses, String resourceType) {
        StreamingOutput streamingOutput = output -> {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(output)) {
                zipOutputStream.putNextEntry(new ZipEntry("struct-events.json"));
                zipOutputStream.write(JsonUtil.writeValueAsString(auditEventV4Responses).getBytes());
                zipOutputStream.closeEntry();
            }
        };
        String fileName = String.format("audit-%s.zip", resourceType);
        return Response.ok(streamingOutput).header("content-disposition", String.format("attachment; filename = %s", fileName)).build();
    }
}
