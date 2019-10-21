package com.sequenceiq.cloudbreak.client;

import static com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryResponse;
import static com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DnsTarget;
import static com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.IPs;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementGrpc;
import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementGrpc.PublicEndpointManagementBlockingStub;
import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningRequest;
import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateRequest;
import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryRequest;
import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryRequest;
import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryResponse;
import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationRequest;
import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationResponse;
import com.google.protobuf.ByteString;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;

import io.grpc.ManagedChannel;

public class ClusterDnsClient {

    private final ManagedChannel channel;

    private final String actorCrn;

    public ClusterDnsClient(ManagedChannel channel, String actorCrn) {
        this.channel = channel;
        this.actorCrn = actorCrn;
    }

    public String createCertificate(String requestId, String accountId, String endpoint, String environment, boolean wildcard, byte[] csr) {
        checkNotNull(requestId);
        checkNotNull(accountId);
        CreateCertificateRequest.Builder requestBuilder = CreateCertificateRequest.newBuilder()
                .setAccountId(accountId)
                .setEnvironment(environment)
                .setEndpoint(endpoint)
                .setAddWildcard(wildcard)
                .setCsr(ByteString.copyFrom(csr));

        return newStub(requestId).createCertificate(requestBuilder.build()).getRequestId();
    }

    public String signCertificate(String requestId, String accountId, String environment, byte[] csr) {
        checkNotNull(requestId);
        checkNotNull(accountId);
        CertificateSigningRequest.Builder requestBuilder = CertificateSigningRequest.newBuilder()
                .setAccountId(accountId)
                .setEnvironmentName(environment)
                .setCsr(ByteString.copyFrom(csr));

        return newStub(requestId).signCertificate(requestBuilder.build()).getWorkflowId();
    }

    public PollCertificateCreationResponse pollCertificateCreation(String requestId, String pollRequestId) {
        checkNotNull(requestId);

        PollCertificateCreationRequest.Builder requestBuilder = PollCertificateCreationRequest.newBuilder()
                .setRequestId(pollRequestId);

        return newStub(requestId).pollCertificateCreation(requestBuilder.build());
    }

    public CreateDnsEntryResponse createDnsEntryWithIp(String requestId, String accountId, String endpoint, String environment, boolean wildcard,
            List<String> ips) {
        checkNotNull(requestId);

        DnsTarget dnsTarget = DnsTarget.newBuilder()
                .setTargetIPs(IPs.newBuilder().addAllIP(ips).build())
                .build();

        CreateDnsEntryRequest.Builder requestBuilder = CreateDnsEntryRequest.newBuilder()
                .setAddWildcard(wildcard)
                .setDnsTarget(dnsTarget)
                .setAccountId(accountId)
                .setEndpoint(endpoint)
                .setEnvironment(environment);

        return newStub(requestId).createDnsEntry(requestBuilder.build());
    }

    public DeleteDnsEntryResponse deleteDnsEntryWithIp(String requestId, String accountId, String endpoint, String environment, boolean wildcard,
            List<String> ips) {
        checkNotNull(requestId);
        DnsTarget dnsTarget = DnsTarget.newBuilder()
                .setTargetIPs(IPs.newBuilder().addAllIP(ips).build())
                .build();
        DeleteDnsEntryRequest.Builder requestBuilder = DeleteDnsEntryRequest.newBuilder()
                .setRemoveWildcard(wildcard)
                .setDnsTarget(dnsTarget)
                .setAccountId(accountId)
                .setEndpoint(endpoint)
                .setEnvironment(environment);
        return newStub(requestId).deleteDnsEntry(requestBuilder.build());
    }

    /**
     * Creates a new stub with the appropriate metadata injecting interceptors.
     *
     * @param requestId the request ID
     * @return the stub
     */
    private PublicEndpointManagementBlockingStub newStub(String requestId) {
        checkNotNull(requestId);
        return PublicEndpointManagementGrpc.newBlockingStub(channel)
                .withInterceptors(new AltusMetadataInterceptor(requestId, actorCrn));
    }
}
