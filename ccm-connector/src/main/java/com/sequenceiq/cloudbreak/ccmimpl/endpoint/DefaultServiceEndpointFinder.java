package com.sequenceiq.cloudbreak.ccmimpl.endpoint;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.ccm.endpoint.DirectServiceEndpointFinder;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpoint;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpointFinder;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpointLookupException;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpointRequest;
import com.sequenceiq.cloudbreak.ccm.exception.CcmException;
import com.sequenceiq.cloudbreak.ccmimpl.util.RetryUtil;

/**
 * Default service endpoint finder, that decides whether to return a direct service endpoint
 * or try to look up a service endpoint via a gRPC call to minasshd. Uses a supplier to
 * obtain the minasshd gRPC endpoint, to facilitate testing and to support dynamic creation
 * of minasshd by the minasshd management service. Implements retrying, as well as caching
 * the minasshd gRPC service endpoint finder to avoid unnecessary calls to the supplier.
 */
@Component
public class DefaultServiceEndpointFinder implements ServiceEndpointFinder {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultServiceEndpointFinder.class);

    /**
     * A cache key for the minasshd endpoint finder.
     */
    private static final String MINASSHD_ENDPOINT_FINDER_CACHE_KEY_FORMAT = "minasshd.endpoint.finder.cache.key.%s/%s";

    /**
     * The default GRPC port.
     */
    private static final int DEFAULT_GRPC_PORT = 8982;

    /**
     * The cache expiration in seconds.
     */
    // JSA TODO make this configurable
    private static final int CACHE_EXPIRATION_IN_SECONDS = 600;

    private final MinaSshdGrpcEndpointSupplier minasshdGrpcEndpointSupplier;

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
    @Inject
    public DefaultServiceEndpointFinder(@Nonnull MinaSshdGrpcEndpointSupplier minasshdGrpcEndpointSupplier, @Nullable Cache<String, Object> cache) {
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

        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String actorCrn = ThreadBasedUserCrnProvider.getUserCrn();
        if (actorCrn == null) {
            throw new IllegalStateException("actor CRN unspecified");
        }

        String targetInstanceId = serviceEndpointRequest.getTargetInstance().getTargetInstanceId();
        String actionDescription = "discover service endpoint for instance " + targetInstanceId;

        return RetryUtil.performWithRetries(
                () -> getServiceEndpoint(accountId, actorCrn, cache, serviceEndpointRequest), actionDescription,
                serviceEndpointRequest.getWaitUntilTime().orElse(null), serviceEndpointRequest.getPollingIntervalInMs(),
                ServiceEndpointLookupException.class,
                () -> new ServiceEndpointLookupException(String.format("Timed out while trying to %s", actionDescription), true),
                LOG);
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private <T extends ServiceEndpoint> T getServiceEndpoint(String accountId, String actorCrn,
            Cache<String, Object> cache, @Nonnull ServiceEndpointRequest<T> serviceEndpointRequest)
            throws ServiceEndpointLookupException {
        String cacheKey = String.format(MINASSHD_ENDPOINT_FINDER_CACHE_KEY_FORMAT, actorCrn, accountId);
        MinaSshdServiceEndpointFinder minaSshdServiceEndpointFinder = getMinaSshdServiceEndpointFinder(cache, cacheKey, accountId, actorCrn);
        try {
            return minaSshdServiceEndpointFinder.getServiceEndpoint(serviceEndpointRequest);
        } catch (ServiceEndpointLookupException e) {
            // JSA Note: a transient exception trying to talk to a good minasshd instance will invalidate the cache here,
            // which is inefficient, but functionally correct since on retry, the call to the minasshd management service should
            // give us back the same minasshd instance. If we did not invalidate the cache on transient exceptions, we would wait
            // until timeout on a bad minasshd instance even if the minasshd management service already knew the instance was bad,
            // which is functionally incorrect.
            synchronized (cache) {
                cache.invalidate(cacheKey);
            }
            throw e;
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private MinaSshdServiceEndpointFinder getMinaSshdServiceEndpointFinder(Cache<String, Object> cache, String cacheKey, String accountId, String actorCrn)
            throws ServiceEndpointLookupException {
        synchronized (cache) {
            try {
                return (MinaSshdServiceEndpointFinder) cache.get(cacheKey, () -> createMinaSshdServiceEndpointFinder(accountId, actorCrn));
            } catch (ExecutionException e) {
                throw (ServiceEndpointLookupException) e.getCause();
            }
        }
    }

    private MinaSshdServiceEndpointFinder createMinaSshdServiceEndpointFinder(String accountId, String actorCrn) throws ServiceEndpointLookupException {
        ServiceEndpoint minasshdGrpcEndpoint = getMinaSshdGrpcServiceEndpoint(accountId, actorCrn);
        return createMinaSshdServiceEndpointFinder(minasshdGrpcEndpoint);
    }

    private ServiceEndpoint getMinaSshdGrpcServiceEndpoint(String accountId, String actorCrn) throws ServiceEndpointLookupException {
        LOG.debug("Requesting endpoint for minasshd gRPC lookup");
        ServiceEndpoint minasshdGrpcEndpoint;
        try {
            minasshdGrpcEndpoint = minasshdGrpcEndpointSupplier.getMinaSshdGrpcServiceEndpoint(actorCrn, accountId);
        } catch (CcmException e) {
            throw wrapCcmExceptionIfNecessary(e);
        }
        return minasshdGrpcEndpoint;
    }

    private MinaSshdServiceEndpointFinder createMinaSshdServiceEndpointFinder(ServiceEndpoint minasshdGrpcEndpoint) {
        String lookupHost = minasshdGrpcEndpoint.getHostEndpoint().getHostAddressString();
        int lookupPort = minasshdGrpcEndpoint.getPort().orElse(DEFAULT_GRPC_PORT);
        LOG.debug("Creating minasshd gRPC service endpoint finder with lookup host: {}, port: {}", lookupHost, lookupPort);
        return new MinaSshdServiceEndpointFinder(lookupHost, lookupPort);
    }

    private ServiceEndpointLookupException wrapCcmExceptionIfNecessary(CcmException e) {
        return (e instanceof ServiceEndpointLookupException)
                ? (ServiceEndpointLookupException) e
                : new ServiceEndpointLookupException(e, e.isRetryable());
    }
}
