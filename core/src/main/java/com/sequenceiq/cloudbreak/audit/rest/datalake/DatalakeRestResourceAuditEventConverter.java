package com.sequenceiq.cloudbreak.audit.rest.datalake;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.auditeventname.rest.RestResourceAuditEventConverter;
import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.rest.LegacyRestCommonService;

@Component("stacksRestResourceAuditEventConverter")
public class DatalakeRestResourceAuditEventConverter implements RestResourceAuditEventConverter {

    @Inject
    private LegacyRestCommonService legacyRestCommonService;

    @Override
    public AuditEventName auditEventName(StructuredRestCallEvent structuredEvent) {
        String method = structuredEvent.getRestCall().getRestRequest().getMethod();
        AuditEventName eventName = null;
        String resourceEvent = structuredEvent.getOperation().getResourceEvent();
        if ("POST".equals(method) || "PUT".equals(method)) {
            if (resourceEvent == null) {
                eventName = AuditEventName.CREATE_DATALAKE_CLUSTER;
            } else {
                eventName = updateRest(resourceEvent);
            }
        } else if ("DELETE".equals(method)) {
            eventName = deletionRest(resourceEvent);
        }
        return eventName;
    }

    private AuditEventName deletionRest(String resourceEvent) {
        if (resourceEvent == null) {
            return AuditEventName.DELETE_DATALAKE_CLUSTER;
        } else if ("instance".equals(resourceEvent) || "instances".equals(resourceEvent)) {
            return AuditEventName.INSTANCE_DELETE_DATALAKE_CLUSTER;
        }
        return null;
    }

    private AuditEventName updateRest(String resourceEvent) {
        if ("retry".equals(resourceEvent)) {
            return AuditEventName.RETRY_DATALAKE_CLUSTER;
        } else if ("stop".equals(resourceEvent)) {
            return AuditEventName.STOP_DATALAKE_CLUSTER;
        } else if ("start".equals(resourceEvent)) {
            return AuditEventName.START_DATALAKE_CLUSTER;
        } else if ("scaling".equals(resourceEvent)) {
            return AuditEventName.RESIZE_DATALAKE_CLUSTER;
        } else if ("maintenance".equals(resourceEvent)) {
            return AuditEventName.MAINTAIN_DATALAKE_CLUSTER;
        } else if ("manual_repair".equals(resourceEvent)) {
            return AuditEventName.MANUAL_REPAIR_DATALAKE_CLUSTER;
        } else if ("rotate_autotls_certificates".equals(resourceEvent)) {
            return AuditEventName.ROTATE_DATALAKE_CLUSTER_CERTIFICATES;
        } else if ("cluster_upgrade".equals(resourceEvent)) {
            return AuditEventName.UPGRADE_DATALAKE_CLUSTER;
        }
        return null;
    }

    @Override
    public boolean shouldAudit(StructuredRestCallEvent structuredRestCallEvent) {
        return true;
    }

    @Override
    public Crn.Service eventSource(StructuredRestCallEvent structuredEvent) {
        return Crn.Service.DATALAKE;
    }

    @Override
    public Map<String, Object> requestParameters(StructuredRestCallEvent structuredEvent) {
        return legacyRestCommonService.addClusterCrnAndNameIfPresent(structuredEvent);
    }
}
