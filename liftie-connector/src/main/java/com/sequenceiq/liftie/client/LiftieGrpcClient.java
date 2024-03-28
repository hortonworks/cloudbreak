package com.sequenceiq.liftie.client;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.CreateClusterRequest;
import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.CreateClusterResponse;
import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.DeleteClusterRequest;
import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.DeleteClusterResponse;
import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.DescribeClusterRequest;
import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.DescribeClusterResponse;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;

import io.grpc.ManagedChannel;

@Component
public class LiftieGrpcClient {

    @Qualifier("liftieManagedChannelWrapper")
    @Inject
    private ManagedChannelWrapper channelWrapper;

    @Inject
    private LiftieGrpcConfig liftieGrpcConfig;

    public DescribeClusterResponse describeCluster(String liftieCrn, String actorCrn) {
        LiftieServiceClient liftieServiceClient = makeClient(channelWrapper.getChannel(), actorCrn);
        return liftieServiceClient.describeCluster(DescribeClusterRequest.newBuilder().setClusterCrn(liftieCrn).build());
    }

    public DeleteClusterResponse deleteCluster(String liftieCrn, String actorCrn) {
        LiftieServiceClient liftieServiceClient = makeClient(channelWrapper.getChannel(), actorCrn);
        return liftieServiceClient.deleteCluster(DeleteClusterRequest.newBuilder().setClusterCrn(liftieCrn).build());
    }

    public CreateClusterResponse createCluster(CreateClusterRequest createClusterRequest, String actorCrn) {
        LiftieServiceClient liftieServiceClient = makeClient(channelWrapper.getChannel(), actorCrn);
        return liftieServiceClient.createCluster(createClusterRequest);
    }

    private LiftieServiceClient makeClient(ManagedChannel channel, String actorCrn) {
        return new LiftieServiceClient(
                channel,
                actorCrn,
                liftieGrpcConfig);
    }
}