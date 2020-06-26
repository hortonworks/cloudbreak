package com.sequenceiq.cloudbreak.audit.converter;

import static com.sequenceiq.cloudbreak.util.ConditionBasedEvaluatorUtil.doIfTrue;
import static com.sequenceiq.cloudbreak.util.UuidUtil.uuidSupplier;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.model.ActorBase;
import com.sequenceiq.cloudbreak.audit.model.ActorCrn;
import com.sequenceiq.cloudbreak.audit.model.ActorService;
import com.sequenceiq.cloudbreak.audit.model.ApiRequestData;
import com.sequenceiq.cloudbreak.audit.model.AuditEvent;
import com.sequenceiq.cloudbreak.audit.model.EventData;
import com.sequenceiq.cloudbreak.audit.model.ServiceEventData;

@Component
public class AuditEventToGrpcAuditEventConverter {

    public AuditProto.AuditEvent convert(AuditEvent source) {
        String id = Optional.ofNullable(source.getId()).orElseGet(uuidSupplier());
        String requestId = Optional.ofNullable(source.getRequestId()).orElseGet(uuidSupplier());
        AuditProto.AuditEvent.Builder auditEventBuilder = prepareBuilderForCreateAuditEvent(source, id, requestId);
        updateAuditEventActor(auditEventBuilder, source.getActor());
        updateAuditEventData(auditEventBuilder, source.getEventData());
        return auditEventBuilder.build();
    }

    private AuditProto.AuditEvent.Builder prepareBuilderForCreateAuditEvent(AuditEvent source, String id, String requestId) {
        AuditProto.AuditEvent.Builder builder = AuditProto.AuditEvent.newBuilder()
                .setId(id)
                .setTimestamp(System.currentTimeMillis())
                .setAccountId(source.getAccountId())
                .setRequestId(requestId)
                .setEventName(source.getEventName().name())
                .setEventSource(source.getEventSource().getName());
        doIfTrue(source.getSourceIp(), StringUtils::isNotEmpty, builder::setSourceIPAddress);
        return builder;
    }

    private void updateAuditEventActor(AuditProto.AuditEvent.Builder auditEventBuilder, ActorBase actorBase) {
        if (actorBase instanceof ActorCrn) {
            ActorCrn actor = (ActorCrn) actorBase;
            auditEventBuilder.setActorCrn(actor.getActorCrn());
        } else if (actorBase instanceof ActorService) {
            ActorService actor = (ActorService) actorBase;
            auditEventBuilder.setActorServiceName(actor.getActorServiceName());
        } else {
            throw new IllegalArgumentException("Actor has an invalid class: " + actorBase.getClass().getName());
        }
    }

    private void updateAuditEventData(AuditProto.AuditEvent.Builder auditEventBuilder, EventData source) {
        if (source == null) {
            return;
        }

        if (source instanceof ServiceEventData) {
            ServiceEventData serviceEventData = (ServiceEventData) source;
            AuditProto.ServiceEventData.Builder serviceEventDataBuilder = AuditProto.ServiceEventData.newBuilder();
            doIfTrue(serviceEventData.getEventDetails(), StringUtils::isNotEmpty, serviceEventDataBuilder::setEventDetails);
            doIfTrue(serviceEventData.getVersion(), StringUtils::isNotEmpty, serviceEventDataBuilder::setDetailsVersion);
            auditEventBuilder.setServiceEventData(serviceEventDataBuilder.build());
        } else if (source instanceof ApiRequestData) {
            ApiRequestData apiRequestData = (ApiRequestData) source;
            AuditProto.ApiRequestData.Builder apiRequestDataBuilder = AuditProto.ApiRequestData.newBuilder()
                    .setMutating(apiRequestData.isMutating());
            doIfTrue(apiRequestData.getApiVersion(), StringUtils::isNotEmpty, apiRequestDataBuilder::setApiVersion);
            doIfTrue(apiRequestData.getRequestParameters(), StringUtils::isNotEmpty, apiRequestDataBuilder::setRequestParameters);
            doIfTrue(apiRequestData.getUserAgent(), StringUtils::isNotEmpty, apiRequestDataBuilder::setUserAgent);
            auditEventBuilder.setApiRequestData(apiRequestDataBuilder.build());
        } else {
            throw new IllegalArgumentException("EventData has an invalid class: " + source.getClass().getName());
        }
    }
}
