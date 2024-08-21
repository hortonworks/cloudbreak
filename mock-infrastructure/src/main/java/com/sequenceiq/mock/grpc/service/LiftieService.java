package com.sequenceiq.mock.grpc.service;

import java.util.Map;
import java.util.Objects;

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
import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.ListClusterItem;
import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.ListClustersRequest;
import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.ListClustersResponse;
import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.ListClustersResponse.Builder;
import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.ValidateCredentialRequest;
import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.ValidateCredentialResponse;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.mock.experience.LiftieExperienceStoreService;
import com.sequenceiq.mock.experience.response.liftie.LiftieClusterView;

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
                .create(request.getEnvironment(), Crn.fromString(request.getEnvironment()).getAccountId(), request.getName(), request.getIsDefault());
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
            LiftieClusterView liftieClusterView = liftieExperienceStoreService.getById(liftie);
            if (liftieClusterView != null) {
                responseObserver.onNext(DescribeClusterResponse.newBuilder().setClusterCrn(clusterCrn)
                                .setClusterName(liftieClusterView.getName()).setClusterId(liftieClusterView.getClusterId())
                        .setStatus(liftieClusterView.getClusterStatus().getStatus()).build());
                responseObserver.onCompleted();
            } else {
                responseObserver.onError(Status.NOT_FOUND.withDescription(liftie + " not found in databased").asException());
            }
        } else {
            getLiftieClusterCrnParsingFailed(responseObserver);
        }
    }

    @Override
    public void listClusters(ListClustersRequest request, StreamObserver<ListClustersResponse> responseObserver) {
        Map<String, LiftieClusterView> clusters = liftieExperienceStoreService.get(request.getEnvNameOrCrn()).getClusters();
        Builder builder = ListClustersResponse.newBuilder();
        for (LiftieClusterView liftieClusterView : clusters.values()) {
            if (!Objects.equals(liftieClusterView.getClusterStatus().getStatus(), "DELETED")) {
                ListClusterItem.Builder cluster = ListClusterItem.newBuilder();
                cluster.setClusterName(liftieClusterView.getName());
                cluster.setClusterId(liftieClusterView.getClusterId());
                cluster.setIsDefault(liftieClusterView.isDefaultCluster());
                Crn envCrn = Crn.fromString(liftieClusterView.getEnv());
                String liftieCrn = liftieClusterView.getEnv()
                        .replace(envCrn.getResource(), liftieClusterView.getClusterId())
                        .replace(envCrn.getService().getName(), "compute")
                        .replace(envCrn.getResourceType().getName(), "cluster");
                cluster.setClusterCrn(liftieCrn);
                cluster.setStatus(liftieClusterView.getClusterStatus().getStatus());
                cluster.setMessage(liftieClusterView.getClusterStatus().getMessage());
                builder.addClusters(cluster);
            }
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void validateCredential(ValidateCredentialRequest request,
            StreamObserver<ValidateCredentialResponse> responseObserver) {
        responseObserver.onNext(ValidateCredentialResponse.newBuilder().setResult("PASSED").build());
        responseObserver.onCompleted();
    }

    private void getLiftieClusterCrnParsingFailed(StreamObserver<DescribeClusterResponse> responseObserver) {
        responseObserver.onError(new RuntimeException("Liftie cluster crn parsing failed"));
    }
}
