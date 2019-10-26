package com.sequenceiq.cloudbreak.ccmimpl.endpoint;

import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.minasshd.MinaSshdGrpc;
import com.cloudera.thunderhead.service.minasshd.MinaSshdProto;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.ccm.endpoint.HostEndpoint;
import com.sequenceiq.cloudbreak.ccm.endpoint.KnownServiceIdentifier;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpoint;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpointFinder;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpointLookupException;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpointRequest;
import com.sequenceiq.cloudbreak.grpc.util.GrpcUtil;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

/**
 * Service endpoint finder that makes a gRPC call to minasshd.
 */
public class MinaSshdServiceEndpointFinder implements ServiceEndpointFinder {

    private static final Logger LOG = LoggerFactory.getLogger(MinaSshdServiceEndpointFinder.class);

    /**
     * The minimum valid port.
     */
    private static final int MIN_PORT = 1;

    /**
     * The maximum valid port.
     */
    private static final int MAX_PORT = 65535;

    /**
     * A map from known service identifiers to the corresponding proto constants.
     */
    private static final Map<KnownServiceIdentifier, MinaSshdProto.EndpointService.Value> ENDPOINT_SERVICE_VALUE_BY_KNOWN_SERVICE_IDENTIFIER;

    static {
        EnumMap<KnownServiceIdentifier, MinaSshdProto.EndpointService.Value> map =
                new EnumMap<KnownServiceIdentifier, MinaSshdProto.EndpointService.Value>(KnownServiceIdentifier.class);
        map.put(KnownServiceIdentifier.GATEWAY, MinaSshdProto.EndpointService.Value.GATEWAY);
        map.put(KnownServiceIdentifier.KNOX, MinaSshdProto.EndpointService.Value.KNOX);
        ENDPOINT_SERVICE_VALUE_BY_KNOWN_SERVICE_IDENTIFIER = Maps.immutableEnumMap(map);
    }

    /**
     * Those statuses that indicate a transient problem.
     */
    private static final Set<Status.Code> RETRYABLE_STATUS_CODES =
            Sets.immutableEnumSet(Status.Code.UNAVAILABLE, Status.Code.NOT_FOUND);

    private final ManagedChannel channel;

    private final MinaSshdGrpc.MinaSshdBlockingStub blockingStub;

    /**
     * Creates a minasshd service endpoint Finder that will use the specified gRPC endpoint.
     *
     * @param lookupHost the hostname or ip for the minasshd gRPC endpoint
     * @param lookupPort the listening port for the minasshd gRPC endpoint
     */
    public MinaSshdServiceEndpointFinder(String lookupHost, int lookupPort) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(lookupHost), "Lookup host is required.");
        Preconditions.checkArgument(lookupPort >= MIN_PORT && lookupPort <= MAX_PORT, "Invalid lookup port, not in [1,65535].");

        channel = ManagedChannelBuilder.forAddress(lookupHost, lookupPort)
                .usePlaintext()
                .maxInboundMessageSize(DEFAULT_MAX_MESSAGE_SIZE)
                .build();
        blockingStub = MinaSshdGrpc.newBlockingStub(channel);
    }

    /**
     * Attempts Orderly Shutdown on Managed Channel.
     */
    @PreDestroy
    protected void shutdown() {
        channel.shutdown();
    }

    @Nonnull
    @Override
    public <T extends ServiceEndpoint> T getServiceEndpoint(@Nonnull ServiceEndpointRequest<T> serviceEndpointRequest)
            throws ServiceEndpointLookupException {

        String instanceId = serviceEndpointRequest.getTargetInstance().getTargetInstanceId();
        if (instanceId.isEmpty()) {
            throw new IllegalArgumentException("Target instance ID is required.");
        }

        Optional<MinaSshdProto.EndpointService.Value> knownServiceIdentifier =
                serviceEndpointRequest.getServiceFamily().getKnownServiceIdentifier()
                        .map(ENDPOINT_SERVICE_VALUE_BY_KNOWN_SERVICE_IDENTIFIER::get);
        if (!knownServiceIdentifier.isPresent()) {
            throw new IllegalArgumentException("Service identifier is required.");
        }

        MinaSshdProto.GetServiceEndpointRequest request = MinaSshdProto.GetServiceEndpointRequest.newBuilder()
                .setEndpointService(knownServiceIdentifier.get())
                .setInstanceId(instanceId).build();

        try {
            MinaSshdProto.GetServiceEndpointResponse response = blockingStub.getServiceEndpoint(request);
            if (response == null) {
                throw new ServiceEndpointLookupException("Got null response from minasshd gRPC call", false);
            } else {
                MinaSshdProto.ServiceEndpoint endpoint = response.getServiceEndpoint();
                if (endpoint == null) {
                    throw new ServiceEndpointLookupException("Got null service endpoint from minasshd gRPC response", false);
                } else {
                    final String endpointHostName = endpoint.getHostName();
                    final int endpointPort = endpoint.getPort();
                    LOG.debug("Mina gRPC lookup returned {}:{} for instance {}.", endpointHostName, endpointPort,
                            instanceId);
                    return serviceEndpointRequest.getServiceFamily().getServiceEndpoint(new HostEndpoint(endpointHostName), endpointPort);
                }
            }
        } catch (StatusRuntimeException e) {
            String message = "Lookup using minasshd gRPC failed: " + e.getMessage();
            Status status = e.getStatus();
            Status.Code code = status.getCode();
            boolean retryable = GrpcUtil.isRetryable(code);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Got status code: {}, retryable: {}", code, retryable);
            }
            throw new ServiceEndpointLookupException(message, e, retryable);
        }
    }
}
