package com.sequenceiq.cloudbreak.ccmimpl.endpoint;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sequenceiq.cloudbreak.ccm.endpoint.DirectServiceEndpointFinder;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpoint;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpointFinder;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpointLookupException;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpointRequest;

/**
 * Default service endpoint finder, that decides whether to return a direct service endpoint
 * or try to look up a service endpoint via a gRPC call to minasshd. Uses a supplier to
 * obtain the minasshd gRPC endpoint, to facilitate testing and to support dynamic creation
 * of minasshd by the minasshd management service. Implements retrying, as well as caching
 * the minasshd gRPC service endpoint finder to avoid unnecessary calls to the supplier.
 */
public class DefaultServiceEndpointFinder implements ServiceEndpointFinder {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultServiceEndpointFinder.class);

    /**
     * A cache key for the minasshd endpoint finder.
     */
    private static final String MINASSHD_ENDPOINT_FINDER_CACHE_KEY = "minasshd.endpoint.finder.cache.key";

    /**
     * The default GRPC port.
     */
    private static final int DEFAULT_GRPC_PORT = 8984;

    /**
     * The cache expiration in seconds.
     */
    // JSA TODO make this configurable
    private static final int CACHE_EXPIRATION_IN_SECONDS = 600;

    private final Supplier<ServiceEndpoint> minasshdGrpcEndpointSupplier;

    private final ServiceEndpointFinder directServiceEndpointFinder = new DirectServiceEndpointFinder();

    /**
     * A cache to avoid expensive gRPC calls.
     */
    private final Cache<String, Object> cache;

    /**
     * Creates a default service endpoint finder with the specified parameters.
     *
     * @param minasshdGrpcEndpointSupplier a supplier for the minasshd gRPC endpoint
     * @param cache                        a cache to avoid expensive gRPC calls
     */
    public DefaultServiceEndpointFinder(@Nonnull Supplier<ServiceEndpoint> minasshdGrpcEndpointSupplier, @Nullable Cache<String, Object> cache) {
        this.minasshdGrpcEndpointSupplier = Objects.requireNonNull(minasshdGrpcEndpointSupplier, "minasshdGrpcEndpointSupplier is null");
        if (cache == null) {
            cache = CacheBuilder.newBuilder()
                    .expireAfterAccess(CACHE_EXPIRATION_IN_SECONDS, TimeUnit.SECONDS)
                    .build();
        }
        this.cache = cache;
    }

    @Nonnull
    @Override
    public <T extends ServiceEndpoint> T getServiceEndpoint(@Nonnull ServiceEndpointRequest<T> serviceEndpointRequest)
            throws ServiceEndpointLookupException, InterruptedException {
        if (serviceEndpointRequest.isDirectAccessRequired()) {
            LOG.debug("Using direct service endpoint finder");
            return directServiceEndpointFinder.getServiceEndpoint(serviceEndpointRequest);
        }

        ServiceEndpointFinder retryingMinaSshdEndpointFinder;
        synchronized (cache) {
            retryingMinaSshdEndpointFinder =
                    (ServiceEndpointFinder) cache.getIfPresent(MINASSHD_ENDPOINT_FINDER_CACHE_KEY);
            if (retryingMinaSshdEndpointFinder == null) {
                LOG.debug("Requesting endpoint for minasshd gRPC lookup");
                ServiceEndpoint minasshdGrpcEndpoint = minasshdGrpcEndpointSupplier.get();
                String lookupHost = minasshdGrpcEndpoint.getHostEndpoint().getHostAddressString();
                int lookupPort = minasshdGrpcEndpoint.getPort().orElse(DEFAULT_GRPC_PORT);
                LOG.debug("Creating minasshd gRPC service endpoint finder with lookup host: {}, port: {}", lookupHost, lookupPort);
                MinaSshdServiceEndpointFinder minaSshdServiceEndpointFinder =
                        new MinaSshdServiceEndpointFinder(lookupHost, lookupPort);
                retryingMinaSshdEndpointFinder = new RetryingServiceEndpointFinder(minaSshdServiceEndpointFinder);
                LOG.debug("Caching retrying minasshd gRPC service endpoint finder");
                cache.put(MINASSHD_ENDPOINT_FINDER_CACHE_KEY, retryingMinaSshdEndpointFinder);
            } else {
                LOG.debug("Using cached retrying minasshd gRPC service endpoint finder");
            }
        }

        try {
            return retryingMinaSshdEndpointFinder.getServiceEndpoint(serviceEndpointRequest);
        } catch (ServiceEndpointLookupException e) {
            // JSA TODO make this more granular, to only throw away the finder if the service is bad,
            // not if the lookup just fails
            synchronized (cache) {
                cache.invalidate(MINASSHD_ENDPOINT_FINDER_CACHE_KEY);
            }
            throw e;
        }
    }
}
