package com.sequenceiq.thunderhead.grpc.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminGrpc;
import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto;
import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateAccountMetadataRequest;
import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateAccountMetadataResponse;
import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListRequest;
import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListResponse;
import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListRequest;
import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListResponse;
import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsRequest;
import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsResponse;
import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventRequest;
import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventResponse;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.thunderhead.grpc.GrpcActorContext;

@Component
public class MockNotificationService extends NotificationAdminGrpc.NotificationAdminImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockNotificationService.class);

    @Override
    public void createOrUpdateDistributionList(
            CreateOrUpdateDistributionListRequest request,
            io.grpc.stub.StreamObserver<CreateOrUpdateDistributionListResponse> responseObserver) {
        LOGGER.info("Calling MOCK createOrUpdateDistributionList");
        CreateOrUpdateDistributionListResponse response = CreateOrUpdateDistributionListResponse.newBuilder()
                .addDistributionListDetails(NotificationAdminProto.DistributionListDetails.newBuilder()
                        .setDistributionListId(request.getDistributionListId())
                        .setResourceCrn(request.getResourceCrn())
                        .build())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void publishTargetedEvent(
            PublishTargetedEventRequest request,
            io.grpc.stub.StreamObserver<PublishTargetedEventResponse> responseObserver) {
        LOGGER.info("Calling MOCK publishTargetedEvent");
        PublishTargetedEventResponse response = PublishTargetedEventResponse.newBuilder()
                .setEventId(request.getEvent().getEventId())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void deleteDistributionList(
            DeleteDistributionListRequest request,
            io.grpc.stub.StreamObserver<DeleteDistributionListResponse> responseObserver) {
        LOGGER.info("Calling MOCK deleteDistributionList");
        DeleteDistributionListResponse response = DeleteDistributionListResponse.newBuilder()
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void listDistributionLists(
            ListDistributionListsRequest request,
            io.grpc.stub.StreamObserver<ListDistributionListsResponse> responseObserver) {
        LOGGER.info("Calling MOCK listDistributionLists");
        ListDistributionListsResponse response = ListDistributionListsResponse.newBuilder()
                .addDistributionLists(NotificationAdminProto.DistributionList.newBuilder()
                        .setResourceCrn(request.getResourceCrn())
                        .setParentResourceCrn(request.getResourceCrn())
                        .setResourceName(request.getResourceCrn())
                        .setDistributionListManagementType(NotificationAdminProto.DistributionListManagementType.Value.USER_MANAGED)
                        .build())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void createOrUpdateAccountMetadata(
            CreateOrUpdateAccountMetadataRequest request,
            io.grpc.stub.StreamObserver<CreateOrUpdateAccountMetadataResponse> responseObserver) {
        LOGGER.info("Calling MOCK createOrUpdateAccountMetadata");
        String accountId = Crn.fromString(GrpcActorContext.ACTOR_CONTEXT.get().getActorCrn()).getAccountId();
        CreateOrUpdateAccountMetadataResponse response = CreateOrUpdateAccountMetadataResponse.newBuilder()
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
