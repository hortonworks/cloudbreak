package com.sequenceiq.liftie.client;

import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicGrpc;
import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicGrpc.LiftiePublicBlockingStub;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.CreateClusterRequest;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.CreateClusterResponse;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.DeleteClusterRequest;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.DeleteClusterResponse;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.DescribeClusterRequest;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.DescribeClusterResponse;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.ListClustersRequest;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.ListClustersResponse;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.ValidateCredentialRequest;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.ValidateCredentialResponse;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.grpc.altus.CallingServiceNameInterceptor;
import com.sequenceiq.cloudbreak.grpc.util.GrpcUtil;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

import io.grpc.ManagedChannel;

public class LiftieServiceClient {

    private final ManagedChannel channel;

    private final String actorCrn;

    private final LiftieGrpcConfig liftieGrpcConfig;

    public LiftieServiceClient(ManagedChannel channel, String actorCrn, LiftieGrpcConfig liftieGrpcConfig) {
        this.channel = channel;
        this.actorCrn = actorCrn;
        this.liftieGrpcConfig = liftieGrpcConfig;
    }

    public DescribeClusterResponse describeCluster(DescribeClusterRequest describeClusterRequest) {
        return newStub().describeCluster(describeClusterRequest);
    }

    public ListClustersResponse listClusters(ListClustersRequest listClustersRequest) {
        return newStub().listClusters(listClustersRequest);
    }

    public CreateClusterResponse createCluster(CreateClusterRequest createClusterRequest) {
        return newStub().createCluster(createClusterRequest);
    }

    public DeleteClusterResponse deleteCluster(DeleteClusterRequest deleteClusterRequest, String envCrn) {
        return newStubWithEnvCrnHeader(envCrn).deleteCluster(deleteClusterRequest);
    }

    public ValidateCredentialResponse validateCredential(ValidateCredentialRequest validateCredentialRequest) {
        return newStub().validateCredential(validateCredentialRequest);
    }

    private LiftiePublicBlockingStub newStubWithEnvCrnHeader(String envCrn) {
        return LiftiePublicGrpc.newBlockingStub(channel)
                .withInterceptors(
                        GrpcUtil.getTimeoutInterceptor(liftieGrpcConfig.getGrpcTimeoutSec()),
                        new EnvCrnMetadataInterceptor(MDCBuilder.getOrGenerateRequestId(), actorCrn, envCrn),
                        new CallingServiceNameInterceptor(liftieGrpcConfig.getCallingServiceName()));
    }

    private LiftiePublicBlockingStub newStub() {
        return LiftiePublicGrpc.newBlockingStub(channel)
                .withInterceptors(
                        GrpcUtil.getTimeoutInterceptor(liftieGrpcConfig.getGrpcTimeoutSec()),
                        new AltusMetadataInterceptor(MDCBuilder.getOrGenerateRequestId(), actorCrn),
                        new CallingServiceNameInterceptor(liftieGrpcConfig.getCallingServiceName()));
    }
}
