package com.sequenceiq.datalake.controller;

import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.datalake.service.SdxEventsService;
import com.sequenceiq.sdx.api.endpoint.SdxEventEndpoint;

@Controller
public class SdxEventController implements SdxEventEndpoint {

    @Inject
    private SdxEventsService sdxEventsService;

    /**
     * Retrieves audit events for the provided Environment CRN.
     *
     * @param environmentCrn a Environment CRN
     * @param types          types of structured events to retrieve
     * @return structured events gathered from datalake and cloudbreak services.
     */
    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public List<CDPStructuredEvent> getAuditEvents(@ResourceCrn String environmentCrn, List<StructuredEventType> types, Integer page, Integer size) {
        return sdxEventsService.getPagedDatalakeAuditEvents(environmentCrn, types, page, size);
    }

    /**
     * Retrieves zipped datalake events for the provided Environment CRN.
     *
     * @param environmentCrn a Environment CRN
     * @param types          types of structured events to retrieve
     * @return zipped datalake events gathered from datalake and cloudbreak services.
     */
    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public Response getDatalakeEventsZip(@ResourceCrn String environmentCrn, List<StructuredEventType> types) {
        List<CDPStructuredEvent> events = sdxEventsService.getDatalakeAuditEvents(environmentCrn, List.of(StructuredEventType.NOTIFICATION));
        return getDatalakeEventsZipResponse(events);
    }

    private Response getDatalakeEventsZipResponse(List<CDPStructuredEvent> events) {
        StreamingOutput streamingOutput = output -> {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(output)) {
                zipOutputStream.putNextEntry(new ZipEntry("struct-events.json"));
                zipOutputStream.write(JsonUtil.writeValueAsString(events).getBytes());
                zipOutputStream.closeEntry();
            }
        };
        String fileName = String.format("datalake-audit.zip");
        return Response.ok(streamingOutput).header("content-disposition", String.format("attachment; filename = %s", fileName)).build();
    }
}
