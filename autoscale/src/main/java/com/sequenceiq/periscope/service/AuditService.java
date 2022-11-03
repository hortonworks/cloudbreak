package com.sequenceiq.periscope.service;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.audit.AuditClient;
import com.sequenceiq.cloudbreak.audit.model.ActorCrn;
import com.sequenceiq.cloudbreak.audit.model.ActorService;
import com.sequenceiq.cloudbreak.audit.model.ApiRequestData;
import com.sequenceiq.cloudbreak.audit.model.AuditEvent;
import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.audit.model.ServiceEventData;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.model.json.Json;

@Service
public class AuditService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditService.class);

    private static final String CLUSTER_CRN = "clusterCrn";

    private static final String AUTOSCALE_ACTION = "autoscaleAction";

    private static final String TIMESTAMP = "timestamp";

    private static final String AUTOSCALE_TRIGGER_STATUS = "autoscaleTriggerStatus";

    @Inject
    private AuditClient auditClient;

    @Value("${info.app.version:}")
    private String periscopeVersion;

    @Async
    public void auditRestApi(Map<String, Object> requestParameters, boolean mutating,
            String userAgent, String userCrn, String accountId, String sourceIp) {
        AuditEvent event = null;
        try {
            ApiRequestData apiRequestData = ApiRequestData.builder()
                    .withApiVersion(periscopeVersion)
                    .withMutating(mutating)
                    .withRequestParameters(new Json(requestParameters).getValue())
                    .withUserAgent(userAgent)
                    .build();
            event = AuditEvent.builder()
                    .withAccountId(accountId)
                    .withActor(ActorCrn.builder().withActorCrn(userCrn).build())
                    .withEventData(apiRequestData)
                    .withEventName(AuditEventName.MANAGE_AUTOSCALE_DATAHUB_CLUSTER)
                    .withEventSource(Crn.Service.DATAHUB)
                    .withSourceIp(sourceIp)
                    .build();
            auditClient.createAuditEvent(event);
        } catch (Exception ex) {
            LOGGER.warn("API Audit event creation failed, error : '{}', event : '{}'", ex.getMessage(), event, ex);
        }
    }

    @Async
    public void auditAutoscaleServiceEvent(ScalingStatus scalingStatus, String auditMessage, String clusterCrn,
            String accountId, Long timestamp) {
        AuditEvent event = null;
        try {
            Map<String, Object> eventDetails = new HashMap<>();
            eventDetails.put(CLUSTER_CRN, clusterCrn);
            eventDetails.put(TIMESTAMP, timestamp);
            eventDetails.put(AUTOSCALE_ACTION, auditMessage);
            eventDetails.put(AUTOSCALE_TRIGGER_STATUS, scalingStatus);

            ServiceEventData serviceEventData = ServiceEventData.builder()
                    .withEventDetails(new Json(eventDetails).getValue())
                    .withVersion(periscopeVersion)
                    .build();

            event = AuditEvent.builder()
                    .withAccountId(accountId)
                    .withEventData(serviceEventData)
                    .withEventName(AuditEventName.AUTOSCALE_DATAHUB_CLUSTER)
                    .withEventSource(Crn.Service.DATAHUB)
                    .withActor(ActorService.builder().withActorServiceName(Crn.Service.DATAHUB.getName()).build())
                    .build();
            auditClient.createAuditEvent(event);
        } catch (Exception ex) {
            LOGGER.warn("Service Audit event creation failed, error : '{}', event : '{}'", ex.getMessage(), event, ex);
        }
    }
}
