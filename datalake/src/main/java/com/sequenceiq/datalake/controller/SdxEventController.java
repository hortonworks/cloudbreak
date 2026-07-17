package com.sequenceiq.datalake.controller;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.datalake.service.SdxEventsService;
import com.sequenceiq.datalake.service.SdxEventsZipService;
import com.sequenceiq.sdx.api.endpoint.SdxEventEndpoint;

@Controller
public class SdxEventController implements SdxEventEndpoint {

    @Inject
    private SdxEventsService sdxEventsService;

    @Inject
    private SdxEventsZipService sdxEventsZipService;

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
     * Events are streamed page-by-page from the database directly into the zip output
     * to avoid holding all events in memory at once.
     *
     * @param environmentCrn a Environment CRN
     * @param types          types of structured events to retrieve
     * @return zipped datalake events gathered from datalake and cloudbreak services.
     */
    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public Response getDatalakeEventsZip(@ResourceCrn String environmentCrn, List<StructuredEventType> types) {
        StreamingOutput streamingOutput = output -> sdxEventsZipService.streamDatalakeAuditEventsAsZip(environmentCrn, output);
        return Response.ok(streamingOutput)
                .header("content-disposition", "attachment; filename = datalake-audit.zip")
                .build();
    }
}
