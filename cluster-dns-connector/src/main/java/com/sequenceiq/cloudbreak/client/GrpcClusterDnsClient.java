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
import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesResponse;
import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningResponse;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.opentracing.Tracer;

@Component
public class GrpcClusterDnsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcClusterDnsClient.class);

    @Inject
    private ClusterDnsConfig clusterDnsConfig;

    @Inject
    private Tracer tracer;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public String signCertificate(String accountId, String environment, byte[] csr, Optional<String> requestId, String resourceCrn) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            ClusterDnsClient client = makeClient(channelWrapper.getChannel(),
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString());
            String requestIdValue = requestId.orElse(UUID.randomUUID().toString());
            LOGGER.info("Fire a create certification request(resource crn: {}) with account id:{}, and requestId: {}", resourceCrn, accountId, requestIdValue);
            Crn crn = Crn.fromString(resourceCrn);
            String signingWorkflowId = client.signCertificate(requestIdValue, accountId, environment, csr, crn);
            LOGGER.info("The workflow id for polling the result of creation: {}", signingWorkflowId);
            return signingWorkflowId;
        }
    }

    public PollCertificateSigningResponse pollCertificateSigning(String signingWorkflowId, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            ClusterDnsClient client = makeClient(channelWrapper.getChannel(),
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString());
            LOGGER.info("Get the result of certification creation with signingWorkflowId: {} and requestId: {}", signingWorkflowId, requestId);
            PollCertificateSigningResponse response = client.pollCertificateSigning(requestId.orElse(UUID.randomUUID().toString()), signingWorkflowId);
            LOGGER.info("The workflow id for polling the result of creation: {}", signingWorkflowId);
            return response;
        }
    }

    public CreateDnsEntryResponse createOrUpdateDnsEntryWithIp(String accountId, String endpoint, String environment, boolean wildcard,
            List<String> ips, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            ClusterDnsClient client = makeClient(channelWrapper.getChannel(),
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString());
            LOGGER.info("Create a dns entry with account id: {} and requestId: {} for ips: {}", accountId, requestId, String.join(",", ips));
            CreateDnsEntryResponse response = client.createDnsEntryWithIp(requestId.orElse(UUID.randomUUID().toString()), accountId, endpoint, environment,
                    wildcard, ips);
            LOGGER.info("Dns entry creation finished for ips {}", String.join(",", ips));
            return response;
        }
    }

    public DeleteDnsEntryResponse deleteDnsEntryWithIp(String accountId, String endpoint, String environment, boolean wildcard,
            List<String> ips, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            ClusterDnsClient client = makeClient(channelWrapper.getChannel(),
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString());
            LOGGER.info("Delete a dns entry with account id: {} and requestId: {} for ips: {}", accountId, requestId, String.join(",", ips));
            DeleteDnsEntryResponse response = client.deleteDnsEntryWithIp(requestId.orElse(UUID.randomUUID().toString()), accountId, endpoint, environment,
                    wildcard, ips);
            LOGGER.info("Dns entry deletion finished for ips {}", String.join(",", ips));
            return response;
        }
    }

    public CreateDnsEntryResponse createOrUpdateDnsEntryWithCloudDns(String accountId, String endpoint, String environment,
            String cloudDns, String hostedZoneId, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            ClusterDnsClient client = makeClient(channelWrapper.getChannel(),
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString());
            LOGGER.info("Create a dns entry with account id: {} and requestId: {} for cloud DNS: {}", accountId, requestId, cloudDns);
            CreateDnsEntryResponse response = client.createDnsEntryWithCloudDns(requestId.orElse(UUID.randomUUID().toString()), accountId,
                    endpoint, environment, cloudDns, hostedZoneId);
            LOGGER.info("Dns entry creation finished for cloud DNS {}", cloudDns);
            return response;
        }
    }

    public DeleteDnsEntryResponse deleteDnsEntryWithCloudDns(String accountId, String endpoint, String environment,
            String cloudDns, String hostedZoneId, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            ClusterDnsClient client = makeClient(channelWrapper.getChannel(),
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString());
            LOGGER.info("Delete a dns entry with account id: {} and requestId: {} for cloud DNS: {}", accountId, requestId, client);
            DeleteDnsEntryResponse response = client.deleteDnsEntryWithCloudDns(requestId.orElse(UUID.randomUUID().toString()), accountId,
                    endpoint, environment, cloudDns, hostedZoneId);
            LOGGER.info("Dns entry deletion finished for cloud DNS {}", cloudDns);
            return response;
        }
    }

    public GenerateManagedDomainNamesResponse generateManagedDomain(String environmentName, List<String> subDomains, String accountId,
            Optional<String> requestId) {
        String requestIdValue = requestId.orElse(UUID.randomUUID().toString());
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            ClusterDnsClient client = makeClient(channelWrapper.getChannel(),
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString());
            LOGGER.info("Generating managed domain names for environment: '{}', subdomain: '{}', account id: '{}', requestId: '{}'", environmentName,
                    String.join(",", subDomains), accountId, requestId);
            GenerateManagedDomainNamesResponse response = client.generateManagedDomainNames(requestIdValue, environmentName, subDomains, accountId);
            LOGGER.info("Domain names generation has been finished, returned values: '{}'", response.getDomainsMap());
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
        return new ClusterDnsClient(channel, accountId, tracer);
    }
}
