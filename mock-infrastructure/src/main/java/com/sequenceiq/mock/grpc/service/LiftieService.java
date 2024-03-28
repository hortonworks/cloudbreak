package com.sequenceiq.mock.grpc.service;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicGrpc.LiftiePublicImplBase;
import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.CommonStatusMessage;
import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.CreateClusterRequest;
import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.CreateClusterResponse;
import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.DeleteClusterRequest;
import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.DeleteClusterResponse;
import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.DescribeClusterRequest;
import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.DescribeClusterResponse;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.mock.experience.LiftieExperienceStoreService;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

@Component
public class LiftieService extends LiftiePublicImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiftieService.class);

    @Inject
    private LiftieExperienceStoreService liftieExperienceStoreService;

    @Override
    public void createCluster(CreateClusterRequest request,
            io.grpc.stub.StreamObserver<CreateClusterResponse> responseObserver) {
        String liftieId = liftieExperienceStoreService
                .create(request.getMetadata().getEnvironmentCrn(), request.getMetadata().getClusterOwner().getAccountId());
        responseObserver.onNext(CreateClusterResponse.newBuilder().setClusterId(liftieId).build());
        responseObserver.onCompleted();
    }

    @Override
    public void deleteCluster(DeleteClusterRequest request, StreamObserver<DeleteClusterResponse> responseObserver) {
        String clusterCrn = request.getClusterCrn();
        Crn crn = Crn.fromString(clusterCrn);
        if (crn != null) {
            String liftie = crn.getResource();
            if (liftieExperienceStoreService.getById(liftie) != null) {
                liftieExperienceStoreService.deleteById(liftie);
                CommonStatusMessage commonStatusMessage = CommonStatusMessage.newBuilder().setStatus("DELETING").build();
                responseObserver.onNext(DeleteClusterResponse.newBuilder().setClusterStatus(commonStatusMessage).build());
                responseObserver.onCompleted();
            } else {
                responseObserver.onError(Status.NOT_FOUND.withDescription("error deleting cluster, cause: [error deleting cluster] info: Cluster " +
                        liftie + " already deleted").asException());
            }
        } else {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Liftie cluster crn parsing failed").asException());
        }
    }

    @Override
    public void describeCluster(DescribeClusterRequest request, StreamObserver<DescribeClusterResponse> responseObserver) {
        String clusterCrn = request.getClusterCrn();
        Crn crn = Crn.fromString(clusterCrn);
        if (crn != null) {
            String liftie = crn.getResource();
            if (liftieExperienceStoreService.getById(liftie) != null) {
                responseObserver.onNext(DescribeClusterResponse.newBuilder().setClusterCrn(clusterCrn)
                        .setStatus(liftieExperienceStoreService.getById(liftie).getClusterStatus().getStatus()).build());
                responseObserver.onCompleted();
            } else {
                responseObserver.onError(Status.NOT_FOUND.withDescription(liftie + " not found in databased").asException());
            }
        } else {
            getLiftieClusterCrnParsingFailed(responseObserver);
        }
    }

    private void getLiftieClusterCrnParsingFailed(StreamObserver<DescribeClusterResponse> responseObserver) {
        responseObserver.onError(new RuntimeException("Liftie cluster crn parsing failed"));
    }
}
