package com.sequenceiq.cloudbreak.client;

import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryResponse;
import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryResponse;
import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationResponse;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

@Component
public class GrpcClusterDnsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcClusterDnsClient.class);

    @Inject
    private ClusterDnsConfig clusterDnsConfig;

    public String createCertificate(String actorCrn, String accountId, String endpoint, String environment, boolean wildcard, byte[] csr,
            Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            ClusterDnsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            String requestIdValue = requestId.orElse(UUID.randomUUID().toString());
            LOGGER.info("Fire a create certification request with account id:{}, and requestId: {}", accountId, requestIdValue);
            String pollRequestId = client.createCertificate(requestIdValue, accountId, endpoint, environment, wildcard, csr);
            LOGGER.info("The request id for polling the result of creation: {}", pollRequestId);
            return pollRequestId;
        }
    }

    public String signCertificate(String actorCrn, String accountId, String environment, byte[] csr,
            Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            ClusterDnsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            String requestIdValue = requestId.orElse(UUID.randomUUID().toString());
            LOGGER.info("Fire a create certification request with account id:{}, and requestId: {}", accountId, requestIdValue);
            String pollRequestId = client.signCertificate(requestIdValue, accountId, environment, csr);
            LOGGER.info("The request id for polling the result of creation: {}", pollRequestId);
            return pollRequestId;
        }
    }

    public PollCertificateCreationResponse pollCreateCertificate(String actorCrn, String pollingRequestId, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            ClusterDnsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.info("Get the result of certification creation with actorCrn:{}, pollingRequestId: {} and requestId: {}",
                    actorCrn, pollingRequestId, requestId);
            PollCertificateCreationResponse response = client.pollCertificateCreation(requestId.orElse(UUID.randomUUID().toString()), pollingRequestId);
            LOGGER.info("The request id for polling the result of creation: {}", pollingRequestId);
            return response;
        }
    }

    public CreateDnsEntryResponse createDnsEntryWithIp(String actorCrn, String accountId, String endpoint, String environment, boolean wildcard,
            List<String> ips, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            ClusterDnsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.info("Create a dns entry with account id: {} and requestId: {} for ips: {}", accountId, requestId, String.join(",", ips));
            CreateDnsEntryResponse response = client.createDnsEntryWithIp(requestId.orElse(UUID.randomUUID().toString()), accountId, endpoint, environment,
                    wildcard, ips);
            LOGGER.info("Dns entry creation finished for ips {}", String.join(",", ips));
            return response;
        }
    }

    public DeleteDnsEntryResponse deleteDnsEntryWithIp(String actorCrn, String accountId, String endpoint, String environment, boolean wildcard,
            List<String> ips, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            ClusterDnsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.info("Delete a dns entry with account id: {} and requestId: {} for ips: {}", accountId, requestId, String.join(",", ips));
            DeleteDnsEntryResponse response = client.deleteDnsEntryWithIp(requestId.orElse(UUID.randomUUID().toString()), accountId, endpoint, environment,
                    wildcard, ips);
            LOGGER.info("Dns entry deletion finished for ips {}", String.join(",", ips));
            return response;
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
