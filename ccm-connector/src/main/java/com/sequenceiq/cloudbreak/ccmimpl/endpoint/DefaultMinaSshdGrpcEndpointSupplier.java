package com.sequenceiq.cloudbreak.ccmimpl.endpoint;

import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto;
import com.google.common.base.Throwables;
import com.sequenceiq.cloudbreak.ccm.endpoint.BaseServiceEndpoint;
import com.sequenceiq.cloudbreak.ccm.endpoint.HostEndpoint;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpoint;
import com.sequenceiq.cloudbreak.ccmimpl.altus.GrpcMinaSshdManagementClient;
import com.sequenceiq.cloudbreak.ccmimpl.altus.config.MinaSshdConfig;

/**
 * Default implementation of minasshd gRPC endpoint supplier that communicates with the minasshd management service
 * to acquire a reference to a minasshd service instance.
 */
@Component
public class DefaultMinaSshdGrpcEndpointSupplier implements MinaSshdGrpcEndpointSupplier {

    @Inject
    private GrpcMinaSshdManagementClient grpcMinaSshdManagementClient;

    @Inject
    private MinaSshdConfig minaSshdConfig;

    @Override
    @Nonnull
    public ServiceEndpoint getMinaSshdGrpcServiceEndpoint(@Nonnull String actorCrn, @Nonnull String accountId) {
        // JSA TODO get request ID from somewhere?
        String requestId = UUID.randomUUID().toString();
        try {
            MinaSshdManagementProto.MinaSshdService minaSshdService = grpcMinaSshdManagementClient.acquireMinaSshdServiceAndWaitUntilReady(
                    requestId,
                    Objects.requireNonNull(actorCrn, "actorCrn is null"),
                    Objects.requireNonNull(accountId, "accountId is null"));

            // Configured host allows port forwarding for local testing
            String minaSshdHostAddressString = minaSshdConfig.getHost().orElse(minaSshdService.getLoadBalancerDnsName());
            return new BaseServiceEndpoint(new HostEndpoint(minaSshdHostAddressString), minaSshdConfig.getPort(), null);
        } catch (Exception e) {
            Throwables.throwIfUnchecked(e);
            throw new RuntimeException(e);
        }
    }
}
