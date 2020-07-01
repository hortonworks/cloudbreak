package com.sequenceiq.caas.grpc.service.audit;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.audit.AuditGrpc;
import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.caas.service.AuditStoreService;
import com.sequenceiq.cloudbreak.common.json.Json;

import io.grpc.stub.StreamObserver;

@Component
public class MockAuditLogService extends AuditGrpc.AuditImplBase {

    @Inject
    private AuditStoreService auditStoreService;

    @Override
    public void createAttemptAuditEvent(AuditProto.CreateAttemptAuditEventRequest request,
            StreamObserver<AuditProto.CreateAttemptAuditEventResponse> responseObserver) {
        super.createAttemptAuditEvent(request, responseObserver);
    }

    @Override
    public void createAuditEvent(AuditProto.CreateAuditEventRequest request, StreamObserver<AuditProto.CreateAuditEventResponse> responseObserver) {
        auditStoreService.store(request.getAuditEvent());
        responseObserver.onNext(AuditProto.CreateAuditEventResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void listEvents(AuditProto.ListEventsRequest request, StreamObserver<AuditProto.ListEventsResponse> responseObserver) {
        List<AuditProto.AuditEvent> auditEvents = auditStoreService.filterByRequest(request);
        List<AuditProto.CdpAuditEvent> responseList = auditEvents.stream().map(a -> {
            AuditProto.CdpAuditEvent.Builder builder = AuditProto.CdpAuditEvent.newBuilder()
                    .setAccountId(a.getAccountId())
                    .setRequestId(a.getRequestId())
                    .setTimestamp(a.getTimestamp())
                    .setEventName(a.getEventName())
                    .setEventSource(a.getEventSource());
            AuditProto.ApiRequestEvent apiRequestEvent = apiRequestEvent(a.getApiRequestData());
            if (apiRequestEvent != null) {
                builder.setApiRequestEvent(apiRequestEvent);
            }
            AuditProto.CdpServiceEvent cdpServiceEvent = serviceEvent(a.getServiceEventData());
            if (cdpServiceEvent != null) {
                builder.setCdpServiceEvent(serviceEvent(a.getServiceEventData()));
            }
            return builder.build();
        })
                .collect(Collectors.toList());
        responseObserver.onNext(AuditProto.ListEventsResponse.newBuilder()
                .addAllAuditEvent(responseList)
                .build());
        responseObserver.onCompleted();
    }

    private AuditProto.CdpServiceEvent serviceEvent(AuditProto.ServiceEventData serviceEventData) {
        String clusterCrn = null;
        if (StringUtils.isNotEmpty(serviceEventData.getEventDetails())) {
            clusterCrn = new Json(serviceEventData.getEventDetails()).getValue("clusterCrn");
        }
        AuditProto.CdpServiceEvent.Builder builder = AuditProto.CdpServiceEvent.newBuilder();
        if (clusterCrn != null) {
            builder.addResourceCrn(clusterCrn);
            return builder.build();
        }
        return null;
    }

    private AuditProto.ApiRequestEvent apiRequestEvent(AuditProto.ApiRequestData apiRequestData) {
        if (StringUtils.isEmpty(apiRequestData.getRequestParameters())) {
            return null;
        }
        return AuditProto.ApiRequestEvent.newBuilder()
                .setMutating(apiRequestData.getMutating())
                .setRequestParameters(apiRequestData.getRequestParameters())
                .setApiVersion(apiRequestData.getApiVersion())
                .setUserAgent(apiRequestData.getUserAgent())
                .build();
    }
}
