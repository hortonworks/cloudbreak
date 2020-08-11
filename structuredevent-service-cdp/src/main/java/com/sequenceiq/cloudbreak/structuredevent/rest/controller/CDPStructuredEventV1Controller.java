package com.sequenceiq.cloudbreak.structuredevent.rest.controller;

import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.CustomPermissionCheck;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventV1Endpoint;
import com.sequenceiq.cloudbreak.structuredevent.service.db.CDPStructuredEventDBService;

@AuthorizationResource
@Controller
public class CDPStructuredEventV1Controller implements CDPStructuredEventV1Endpoint {

    @Inject
    private CDPStructuredEventDBService structuredEventDBService;

    @Override
    @CustomPermissionCheck
    public List<CDPStructuredEvent> getAuditEvents(@ResourceCrn String resourceCrn, Integer page, Integer size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return structuredEventDBService
                .getPagedEventsOfResource(List.of(StructuredEventType.REST, StructuredEventType.FLOW, StructuredEventType.NOTIFICATION), resourceCrn, pageable)
                .getContent();
    }

    @Override
    @CustomPermissionCheck
    public Response getAuditEventsZip(@ResourceCrn String resourceCrn) {
        Collection<CDPStructuredNotificationEvent> events = structuredEventDBService.getNotificationEventsOfResource(StructuredEventType.NOTIFICATION,
                resourceCrn);
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
}
