package com.sequenceiq.cloudbreak.audit.converter.auditeventname.rest.datahub;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.audit.converter.auditeventname.rest.RestResourceAuditEventConverter;
import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;

@Component
public class DatahubRestResourceAuditEventConverter implements RestResourceAuditEventConverter {

    @Override
    public AuditEventName auditEventName(StructuredRestCallEvent structuredEvent) {
        String method = structuredEvent.getRestCall().getRestRequest().getMethod();
        AuditEventName eventName = null;
        String resourceEvent = structuredEvent.getOperation().getResourceEvent();
        if ("POST".equals(method) || "PUT".equals(method)) {
            if (resourceEvent == null) {
                eventName = AuditEventName.CREATE_DATAHUB_CLUSTER;
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
            return AuditEventName.DELETE_DATAHUB_CLUSTER;
        } else if ("instance".equals(resourceEvent) || "instances".equals(resourceEvent)) {
            return AuditEventName.INSTANCE_DELETE_DATAHUB_CLUSTER;
        }
        return null;
    }

    private AuditEventName updateRest(String resourceEvent) {
        if ("retry".equals(resourceEvent)) {
            return AuditEventName.RETRY_DATAHUB_CLUSTER;
        } else if ("stop".equals(resourceEvent)) {
            return AuditEventName.STOP_DATAHUB_CLUSTER;
        } else if ("start".equals(resourceEvent)) {
            return AuditEventName.START_DATAHUB_CLUSTER;
        } else if ("scaling".equals(resourceEvent)) {
            return AuditEventName.RESIZE_DATAHUB_CLUSTER;
        } else if ("maintenance".equals(resourceEvent)) {
            return AuditEventName.MAINTAIN_DATAHUB_CLUSTER;
        } else if ("manual_repair".equals(resourceEvent)) {
            return AuditEventName.MANUAL_REPAIR_DATAHUB_CLUSTER;
        }
        return null;
    }

    @Override
    public boolean shouldAudit(StructuredRestCallEvent structuredRestCallEvent) {
        return true;
    }

    @Override
    public Crn.Service eventSource(StructuredRestCallEvent structuredEvent) {
        return Crn.Service.DATAHUB;
    }
}
