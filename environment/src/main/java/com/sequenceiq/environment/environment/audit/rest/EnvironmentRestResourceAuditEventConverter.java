package com.sequenceiq.environment.environment.audit.rest;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.rest.CDPRestCommonService;
import com.sequenceiq.cloudbreak.structuredevent.service.audit.auditeventname.rest.CDPRestResourceAuditEventConverter;

@Component
public class EnvironmentRestResourceAuditEventConverter implements CDPRestResourceAuditEventConverter {

    @Inject
    private CDPRestCommonService restCommonService;

    @Override
    public AuditEventName auditEventName(CDPStructuredRestCallEvent structuredEvent) {
        String method = structuredEvent.getRestCall().getRestRequest().getMethod();
        AuditEventName eventName = null;
        String resourceEvent = structuredEvent.getOperation().getResourceEvent();
        if ("POST".equals(method) || "PUT".equals(method)) {
            if (resourceEvent == null) {
                eventName = AuditEventName.CREATE_ENVIRONMENT;
            } else {
                eventName = updateRest(resourceEvent);
            }
        } else if ("DELETE".equals(method)) {
            eventName = deletionRest(resourceEvent, structuredEvent.getOperation());
        }
        return eventName;
    }

    private AuditEventName deletionRest(String resourceEvent, CDPOperationDetails operationDetails) {
        if (StringUtils.isEmpty(resourceEvent)
                || resourceEvent.equals(operationDetails.getResourceName())
                || resourceEvent.equals(operationDetails.getResourceCrn())) {
            return AuditEventName.DELETE_ENVIRONMENT;
        }
        return null;
    }

    private AuditEventName updateRest(String resourceEvent) {
        AuditEventName auditEventName = null;
        if ("stop".equals(resourceEvent)) {
            auditEventName = AuditEventName.STOP_ENVIRONMENT;
        } else if ("start".equals(resourceEvent)) {
            auditEventName = AuditEventName.START_ENVIRONMENT;
        }
        return auditEventName;
    }

    @Override
    public boolean shouldAudit(CDPStructuredRestCallEvent structuredRestCallEvent) {
        return true;
    }

    @Override
    public Crn.Service eventSource(CDPStructuredRestCallEvent structuredEvent) {
        return Crn.Service.ENVIRONMENTS;
    }

    @Override
    public Map<String, String> requestParameters(CDPStructuredRestCallEvent structuredEvent) {
        return restCommonService.collectCrnAndNameIfPresent(structuredEvent.getRestCall(), structuredEvent.getOperation(), Collections.emptyMap(), "", "");
    }
}
