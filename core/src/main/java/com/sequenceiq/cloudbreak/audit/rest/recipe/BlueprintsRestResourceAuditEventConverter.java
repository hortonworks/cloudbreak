package com.sequenceiq.cloudbreak.audit.rest.recipe;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.structuredevent.auditeventname.rest.RestResourceAuditEventConverter;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.rest.LegacyRestCommonService;

@Component
public class BlueprintsRestResourceAuditEventConverter implements RestResourceAuditEventConverter {

    @Inject
    private LegacyRestCommonService legacyRestCommonService;

    @Override
    public AuditEventName auditEventName(StructuredRestCallEvent structuredEvent) {
        String method = structuredEvent.getRestCall().getRestRequest().getMethod();
        AuditEventName eventName = null;
        String resourceEvent = structuredEvent.getOperation().getResourceEvent();
        if ("POST".equals(method) && resourceEvent == null) {
            eventName = AuditEventName.CREATE_BLUEPRINT;
        } else if ("DELETE".equals(method)) {
            eventName = AuditEventName.DELETE_BLUEPRINT;
        }
        return eventName;
    }

    @Override
    public boolean shouldAudit(StructuredRestCallEvent structuredRestCallEvent) {
        return true;
    }

    @Override
    public Crn.Service eventSource(StructuredRestCallEvent structuredEvent) {
        return Crn.Service.DATAHUB;
    }

    @Override
    public Map<String, Object> requestParameters(StructuredRestCallEvent structuredEvent) {
        return legacyRestCommonService.addClusterCrnAndNameIfPresent(structuredEvent);
    }
}
