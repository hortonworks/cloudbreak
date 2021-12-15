package com.sequenceiq.datalake.controller;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
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
    @CheckPermissionByAccount(action = AuthorizationResourceAction.DESCRIBE_DATALAKE)
    public List<CDPStructuredEvent> getAuditEvents(String environmentCrn, List<StructuredEventType> types, Integer page, Integer size) {
        return sdxEventsService.getDatalakeAuditEvents(environmentCrn, types, page, size);
    }
}
