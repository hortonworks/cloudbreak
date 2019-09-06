package com.sequenceiq.cloudbreak.client;

import static com.google.common.base.Preconditions.checkNotNull;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementGrpc;
import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementGrpc.PublicEndpointManagementBlockingStub;
import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateCertificateRequest;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;

import io.grpc.ManagedChannel;

@Component
public class ClusterDnsClient {

    private final ManagedChannel channel;

    private final String actorCrn;

    public ClusterDnsClient(ManagedChannel channel, String actorCrn) {
        this.channel = channel;
        this.actorCrn = actorCrn;
    }

    public String createCertificate(String requestId, String accountId) {
        checkNotNull(requestId);
        checkNotNull(accountId);

        CreateCertificateRequest.Builder requestBuilder = CreateCertificateRequest.newBuilder()
                .setAccountId(accountId);

        return newStub(requestId).createCertificate(requestBuilder.build()).getRequestId();
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
