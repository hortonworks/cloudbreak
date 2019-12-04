package com.sequenceiq.cloudbreak.ccmimpl.altus;

import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto;
import com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.MinaSshdService;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.ccm.exception.CcmException;
import com.sequenceiq.cloudbreak.ccmimpl.altus.config.MinaSshdManagementClientConfig;
import com.sequenceiq.cloudbreak.ccmimpl.altus.config.MinaSshdManagementConfig;
import com.sequenceiq.cloudbreak.ccmimpl.util.RetryUtil;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

@Component
public class GrpcMinaSshdManagementClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcMinaSshdManagementClient.class);

    @VisibleForTesting
    @Autowired(required = false)
    Clock clock = Clock.systemUTC();

    @Inject
    private MinaSshdManagementConfig minaSshdManagementConfig;

    @Inject
    private MinaSshdManagementClientConfig minaSshdManagementClientConfig;

    /**
     * Attempts to acquire a minasshd service for the specified account. If it is not available immediately,
     * polls until it is acquires a ready service, is interrupted, times out, or there are no pending minasshd service instances.
     *
     * @param requestId the request ID for the request
     * @param actorCrn  the actor CRN
     * @param accountId the account ID
     * @return the minasshd service
     * @throws InterruptedException if the thread is interrupted
     * @throws CcmException         if the initial acquisition fails, if the timeout is reached, or if all minasshd service instances are in a FAILED state
     */
    public MinaSshdService acquireMinaSshdServiceAndWaitUntilReady(String requestId, String actorCrn, String accountId)
            throws InterruptedException, CcmException {

        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            MinaSshdManagementClient client = makeClient(channelWrapper.getChannel(), actorCrn);

            ZonedDateTime waitUntilTime = ZonedDateTime.now(clock).plus(minaSshdManagementClientConfig.getTimeoutMs(), ChronoUnit.MILLIS);
            int pollingIntervalMillis = minaSshdManagementClientConfig.getPollingIntervalMs();

            String actionDescription = "acquire MinaSSHD service for accountId " + accountId;
            Supplier<CcmException> timeoutExceptionSupplier = () -> new CcmException(String.format("Timed out while trying to %s", actionDescription), true);

            // First call acquireMinaSshdService, with retries in case of transient failures.
            MinaSshdService initialService = RetryUtil.performWithRetries(
                    () -> client.acquireMinaSshdService(requestId, accountId),
                    actionDescription, waitUntilTime, pollingIntervalMillis, CcmException.class, timeoutExceptionSupplier,
                    LOGGER);

            // If the minasshd service was pre-existing, it is in the initial call result
            return getValidMinaSshdService(actionDescription, initialService)
                    // Otherwise, poll until the minasshd service is in a final state (STARTED or FAILED) or we time out
                    .orElse(awaitValidMinaSshdService(
                            () -> client.listMinaSshdServices(requestId, accountId, Collections.singletonList(initialService.getMinaSshdServiceId())),
                            actionDescription, waitUntilTime, pollingIntervalMillis, timeoutExceptionSupplier)
                            .orElseThrow(() -> new CcmException(String.format("Failed while trying to %s", actionDescription), false)));
        }
    }

    /**
     * Returns an optional wrapping the specified service if has a status of {@code STARTED} and is ready;
     * throws an exception if it has a status of {@code FAILED}; or returns an empty optional otherwise.
     *
     * @param actionDescription the action description for logging and exception messages
     * @param service           the service
     * @return an optional wrapping the specified service if it is ready and has a status of {@code STARTED}, or an empty optional
     * @throws CcmException if the service is ready and has a status of {@code FAILED}
     */
    private Optional<MinaSshdService> getValidMinaSshdService(String actionDescription, MinaSshdService service) throws CcmException {
        switch (service.getStatus()) {
            case STARTED:
                if (service.getReady()) {
                    return Optional.of(service);
                }
                break;
            case FAILED:
                throw new CcmException(String.format("Failed while trying to %s", actionDescription), false);
            default:
                break;
        }
        return Optional.empty();
    }

    /**
     * Polls trying to find a service which is ready and has a status of {@code STARTED}.
     *
     * @param listAction               the action to list the minasshd services (must return at most 1 service)
     * @param actionDescription        the action description, for logging and exception messages
     * @param waitUntilTime            the latest time to wait to perform the action
     * @param pollingIntervalMillis    the polling interval in milliseconds
     * @param timeoutExceptionSupplier a supplier of exceptions for when all attempts fail
     * @return an optional wrapping a service that is ready and has a status of {@code STARTED}, or an empty optional if there are no pending services
     * @throws InterruptedException if the thread is interrupted
     * @throws CcmException         if the operation times out with services still pending, or if a non-transient error occurs
     */
    private Optional<MinaSshdService> awaitValidMinaSshdService(Callable<List<MinaSshdService>> listAction, String actionDescription,
            ZonedDateTime waitUntilTime, int pollingIntervalMillis,
            Supplier<CcmException> timeoutExceptionSupplier)
            throws InterruptedException, CcmException {
        long waitUntilTimeMillis = waitUntilTime.toInstant().toEpochMilli();

        while (true) {

            // Call listMinaSshdServices, with retries in case of transient failures.
            List<MinaSshdService> minaSshdServices = RetryUtil.performWithRetries(
                    listAction, actionDescription, waitUntilTime, pollingIntervalMillis,
                    CcmException.class, timeoutExceptionSupplier,
                    LOGGER);

            MinaSshdService service;
            switch (minaSshdServices.size()) {
                case 0:
                    return Optional.empty();
                case 1:
                    service = minaSshdServices.get(0);
                    break;
                default:
                    throw new IllegalStateException("listAction returned multiple services");
            }

            switch (service.getStatus()) {
                case STARTED:
                    if (service.getReady()) {
                        return Optional.of(service);
                    }
                    break;
                case FAILED:
                    return Optional.empty();
                default:
                    break;
            }

            if (Thread.interrupted()) {
                throw new InterruptedException(String.format("Interrupted while trying to %s", actionDescription));
            }

            long delay = Math.min(pollingIntervalMillis, waitUntilTimeMillis - clock.millis());
            if (delay <= 0) {
                throw timeoutExceptionSupplier.get();
            }
            Thread.sleep(delay);
        }
    }

    /**
     * Wraps call to generateAndRegisterSshTunnelingKeyPair, with retries to tolerate transient failures.
     *
     * @param requestId         the request ID for the request
     * @param actorCrn          the actor CRN
     * @param accountId         the account ID
     * @param minaSshdServiceId the minasshd service ID
     * @param keyId             the key ID
     * @return the response containing the key pair
     * @throws CcmException         if an exception occurs
     * @throws InterruptedException if the action is interrupted
     */
    public MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairResponse generateAndRegisterSshTunnelingKeyPair(
            String requestId, String actorCrn, String accountId, String minaSshdServiceId, String keyId) throws CcmException, InterruptedException {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            MinaSshdManagementClient client = makeClient(channelWrapper.getChannel(), actorCrn);

            ZonedDateTime waitUntilTime = ZonedDateTime.now(clock).plus(minaSshdManagementClientConfig.getTimeoutMs(), ChronoUnit.MILLIS);
            int pollingIntervalMillis = minaSshdManagementClientConfig.getPollingIntervalMs();

            String actionDescription = "generate tunneling key pair for accountId " + accountId;
            Supplier<CcmException> timeoutExceptionSupplier = () -> new CcmException(String.format("Timed out while trying to %s", actionDescription), true);

            return RetryUtil.performWithRetries(
                    () -> client.generateAndRegisterSshTunnelingKeyPair(requestId, accountId, minaSshdServiceId, keyId), actionDescription,
                    waitUntilTime, pollingIntervalMillis,
                    CcmException.class, timeoutExceptionSupplier,
                    LOGGER);
        }
    }

    /**
     * Wraps call to unregisterSshTunnelingKey, with retries to tolerate transient failures.
     *
     * @param requestId the request ID for the request
     * @param actorCrn  the actor CRN
     * @param accountId the account ID
     * @param keyId     the key ID
     * @param minaSshdServiceId minaSshdServiceId
     * @return the response
     * @throws CcmException         if an exception occurs
     * @throws InterruptedException if the action is interrupted
     */
    public MinaSshdManagementProto.UnregisterSshTunnelingKeyResponse unregisterSshTunnelingKey(
            String requestId, String actorCrn, String accountId, String keyId, String minaSshdServiceId) throws CcmException, InterruptedException {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            MinaSshdManagementClient client = makeClient(channelWrapper.getChannel(), actorCrn);

            ZonedDateTime waitUntilTime = ZonedDateTime.now(clock).plus(minaSshdManagementClientConfig.getTimeoutMs(), ChronoUnit.MILLIS);
            int pollingIntervalMillis = minaSshdManagementClientConfig.getPollingIntervalMs();

            String actionDescription = "deregister tunneling key " + keyId;
            Supplier<CcmException> timeoutExceptionSupplier = () -> new CcmException(String.format("Timed out while trying to %s", actionDescription), true);

            return RetryUtil.performWithRetries(
                    () -> client.unregisterSshTunnelingKey(requestId, minaSshdServiceId, keyId), actionDescription,
                    waitUntilTime, pollingIntervalMillis,
                    CcmException.class, timeoutExceptionSupplier,
                    LOGGER);
        }
    }

    private ManagedChannelWrapper makeWrapper() {
        return new ManagedChannelWrapper(
                ManagedChannelBuilder.forAddress(minaSshdManagementConfig.getEndpoint(), minaSshdManagementConfig.getPort())
                        .usePlaintext()
                        .maxInboundMessageSize(DEFAULT_MAX_MESSAGE_SIZE)
                        .build());
    }

    private MinaSshdManagementClient makeClient(ManagedChannel channel, String actorCrn) {
        return new MinaSshdManagementClient(channel, actorCrn, minaSshdManagementClientConfig);
    }
}
