package com.sequenceiq.periscope.service;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

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
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
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
            String userAgent, CloudbreakUser user, String requestId, String sourceIp) {
        String userCrn = user.getUserCrn();
        ThreadBasedUserCrnProvider.doAs(userCrn, () -> {
            String tenant = user.getTenant();
            MDCBuilder.addRequestId(requestId);
            AuditEvent event = null;
            try {
                ApiRequestData apiRequestData = ApiRequestData.builder()
                        .withApiVersion(periscopeVersion)
                        .withMutating(mutating)
                        .withRequestParameters(new Json(requestParameters).getValue())
                        .withUserAgent(userAgent)
                        .build();
                event = AuditEvent.builder()
                        .withAccountId(tenant)
                        .withActor(ActorCrn.builder().withActorCrn(userCrn).build())
                        .withEventData(apiRequestData)
                        .withEventName(AuditEventName.MANAGE_AUTOSCALE_DATAHUB_CLUSTER)
                        .withEventSource(Crn.Service.AUTOSCALE)
                        .withSourceIp(sourceIp)
                        .withRequestId(requestId)
                        .build();
                auditClient.createAuditEvent(event);
                LOGGER.info("Audit event has been sent with request id: {}", requestId);
            } catch (Exception ex) {
                LOGGER.warn("API Audit event creation failed, error : '{}', event : '{}'", ex.getMessage(), event, ex);
            }
        });
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
                    .withEventSource(Crn.Service.AUTOSCALE)
                    .withActor(ActorService.builder().withActorServiceName(Crn.Service.DATAHUB.getName()).build())
                    .build();
            auditClient.createAuditEvent(event);
        } catch (Exception ex) {
            LOGGER.warn("Service Audit event creation failed, error : '{}', event : '{}'", ex.getMessage(), event, ex);
        }
    }
}
