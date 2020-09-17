package com.sequenceiq.environment.credential.audit.rest;

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
public class CredentialRestResourceAuditEventConverter implements CDPRestResourceAuditEventConverter {

    @Inject
    private CDPRestCommonService restCommonService;

    @Override
    public AuditEventName auditEventName(CDPStructuredRestCallEvent structuredEvent) {
        String method = structuredEvent.getRestCall().getRestRequest().getMethod();
        AuditEventName eventName = null;
        String resourceEvent = structuredEvent.getOperation().getResourceEvent();
        if ("POST".equals(method) && resourceEvent == null) {
            eventName = AuditEventName.CREATE_CREDENTIAL;
        } else if ("PUT".equals(method) && resourceEvent == null) {
            eventName = AuditEventName.MODIFY_CREDENTIAL;
        } else if ("DELETE".equals(method)) {
            eventName = deletionRest(resourceEvent, structuredEvent.getOperation());
        }
        return eventName;
    }

    private AuditEventName deletionRest(String resourceEvent, CDPOperationDetails operationDetails) {
        if (StringUtils.isEmpty(resourceEvent)
                || resourceEvent.equals(operationDetails.getResourceName())
                || resourceEvent.equals(operationDetails.getResourceCrn())) {
            return AuditEventName.DELETE_CREDENTIAL;
        }
        return null;
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
        return restCommonService.addClusterCrnAndNameIfPresent(structuredEvent.getRestCall(), structuredEvent.getOperation(), Collections.emptyMap(), "", "");
    }
}
