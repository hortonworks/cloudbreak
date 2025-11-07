package com.sequenceiq.liftie.client;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.CreateClusterRequest;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.CreateClusterResponse;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.DeleteClusterRequest;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.DeleteClusterResponse;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.DescribeClusterRequest;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.DescribeClusterResponse;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.ListClusterItem;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.ListClustersRequest;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.ListClustersResponse;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.ValidateCredentialRequest;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.ValidateCredentialResponse;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;

import io.grpc.ManagedChannel;

@Component
public class LiftieGrpcClient {

    @Qualifier("liftieManagedChannelWrapper")
    @Inject
    private ManagedChannelWrapper channelWrapper;

    @Inject
    private LiftieGrpcConfig liftieGrpcConfig;

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public DescribeClusterResponse describeCluster(String liftieCrn, String envCrn, String actorCrn) {
        LiftieServiceClient liftieServiceClient = makeClient(channelWrapper.getChannel(), actorCrn);
        return liftieServiceClient.describeCluster(DescribeClusterRequest.newBuilder().setClusterCrn(liftieCrn).build(), envCrn);
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public List<ListClusterItem> listAuxClusters(String envCrn, String actorCrn) {
        LiftieServiceClient liftieServiceClient = makeClient(channelWrapper.getChannel(), actorCrn);
        ListClustersResponse listClustersResponse = liftieServiceClient.listClusters(ListClustersRequest.newBuilder().setEnvNameOrCrn(envCrn).build());
        return listClustersResponse.getClustersList().stream()
                        .filter(listClusterItem -> !listClusterItem.getIsDefault())
                        .toList();
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public DeleteClusterResponse deleteCluster(String liftieCrn, String actorCrn, String envCrn, boolean force) {
        LiftieServiceClient liftieServiceClient = makeClient(channelWrapper.getChannel(), actorCrn);
        return liftieServiceClient.deleteCluster(DeleteClusterRequest.newBuilder().setClusterCrn(liftieCrn).setSkipValidation(true)
                .setForce(force).build(), envCrn);
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public CreateClusterResponse createCluster(CreateClusterRequest createClusterRequest, String actorCrn) {
        LiftieServiceClient liftieServiceClient = makeClient(channelWrapper.getChannel(), actorCrn);
        return liftieServiceClient.createCluster(createClusterRequest);
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public ValidateCredentialResponse validateCredential(ValidateCredentialRequest validateCredentialRequest, String envCrn, String actorCrn) {
        LiftieServiceClient liftieServiceClient = makeClient(channelWrapper.getChannel(), actorCrn);
        return liftieServiceClient.validateCredential(validateCredentialRequest, envCrn);
    }

    private LiftieServiceClient makeClient(ManagedChannel channel, String actorCrn) {
        return new LiftieServiceClient(
                channel,
                actorCrn,
                liftieGrpcConfig);
    }
}