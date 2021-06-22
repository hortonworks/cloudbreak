package com.sequenceiq.cloudbreak.structuredevent.service.audit;

import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.audit.model.EventData;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;

public interface LegacyEventDataExtractor<T extends StructuredEvent> {

    String USER_CRN = "userCrn";

    String CLUSTER_CRN = "clusterCrn";

    String CLUSTER_NAME = "clusterName";

    String TIMESTAMP = "timestamp";

    String ENVIRONMENT_CRN = "environmentCrn";

    String FLOW_STATE = "flowState";

    String FLOW_ID = "flowId";

    EventData eventData(T structuredEvent);

    AuditEventName eventName(T structuredEvent);

    Crn.Service eventSource(T structuredEvent);

    String sourceIp(T structuredEvent);

    boolean shouldAudit(StructuredEvent structuredEvent);
}
