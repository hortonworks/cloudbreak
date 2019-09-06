package com.sequenceiq.cloudbreak.client;

import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

@Component
public class GrpcClusterDnsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcClusterDnsClient.class);

    @Inject
    private ClusterDnsConfig clusterDnsConfig;

    @Inject
    private ClusterDnsClient clusterDnsClient;

    public String createCertificate(String actorCrn, String accountId, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            ClusterDnsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.debug("Fire a create certification request with account id:{}, and requestId: {}", accountId, requestId);
            String pollRequestId = client.createCertificate(requestId.orElse(UUID.randomUUID().toString()), accountId);
            LOGGER.debug("The request id for polling the result of creation: {}", pollRequestId);
            return pollRequestId;
        }
    }

    private ManagedChannelWrapper makeWrapper() {
        return new ManagedChannelWrapper(
                ManagedChannelBuilder.forAddress(clusterDnsConfig.getEndpoint(), clusterDnsConfig.getPort())
                        .usePlaintext()
                        .maxInboundMessageSize(DEFAULT_MAX_MESSAGE_SIZE)
                        .build());
    }

    private ClusterDnsClient makeClient(ManagedChannel channel, String accountId) {
        return new ClusterDnsClient(channel, accountId);
    }
}
