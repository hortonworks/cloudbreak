package com.sequenceiq.cloudbreak.client;

import static com.google.common.base.Preconditions.checkNotNull;

import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementGrpc;
import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementGrpc.PublicEndpointManagementBlockingStub;
import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateRequest;
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

    public PollCertificateCreationResponse pollCertificateCreation(String requestId, String pollRequestId) {
        checkNotNull(requestId);

        PollCertificateCreationRequest.Builder requestBuilder = PollCertificateCreationRequest.newBuilder()
                .setRequestId(pollRequestId);

        return newStub(requestId).pollCertificateCreation(requestBuilder.build());
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
