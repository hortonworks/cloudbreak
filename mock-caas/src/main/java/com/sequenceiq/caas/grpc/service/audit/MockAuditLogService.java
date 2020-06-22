package com.sequenceiq.caas.grpc.service.audit;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.audit.AuditGrpc;
import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.caas.service.AuditStoreService;

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
        responseObserver.onNext(AuditProto.ListEventsResponse.newBuilder()
                .addAllAuditEvent(auditEvents.stream().map(a ->
                        AuditProto.CdpAuditEvent.newBuilder()
                                .setAccountId(a.getAccountId())
                                .setRequestId(a.getRequestId())
                                .setTimestamp(a.getTimestamp())
                                .setEventName(a.getEventName())
                                .setEventSource(a.getEventSource())
                                .setApiRequestEvent(apiRequestEvent(a.getApiRequestData()))
                                .setCdpServiceEvent(serviceEvent(a.getServiceEventData()))
                                .build())
                        .collect(Collectors.toList()))
                .build());
        responseObserver.onCompleted();
    }

    private AuditProto.CdpServiceEvent serviceEvent(AuditProto.ServiceEventData serviceEventData) {
        return AuditProto.CdpServiceEvent.newBuilder()
                .addResourceCrn(serviceEventData.getEventDetails())
                .build();
    }

    private AuditProto.ApiRequestEvent apiRequestEvent(AuditProto.ApiRequestData apiRequestData) {
        return AuditProto.ApiRequestEvent.newBuilder()
                .setMutating(apiRequestData.getMutating())
                .setRequestParameters(apiRequestData.getRequestParameters())
                .setApiVersion(apiRequestData.getApiVersion())
                .setUserAgent(apiRequestData.getUserAgent())
                .build();
    }
}
