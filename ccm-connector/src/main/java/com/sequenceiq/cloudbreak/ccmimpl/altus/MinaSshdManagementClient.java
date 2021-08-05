package com.sequenceiq.cloudbreak.ccmimpl.altus;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementGrpc;
import com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementGrpc.MinaSshdManagementBlockingStub;
import com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceRequest;
import com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.AcquireMinaSshdServiceResponse;
import com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairRequest;
import com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairResponse;
import com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesRequest;
import com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.ListMinaSshdServicesResponse;
import com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.MinaSshdService;
import com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyRequest;
import com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.UnregisterSshTunnelingKeyResponse;
import com.sequenceiq.cloudbreak.ccm.exception.CcmException;
import com.sequenceiq.cloudbreak.ccmimpl.altus.config.MinaSshdManagementClientConfig;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.grpc.util.GrpcUtil;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.opentracing.Tracer;

/**
 * A wrapper to the GRPC minasshd management service, that handles setting up the appropriate context-propagating interceptors,
 * hides some boilerplate, and takes care of retries.
 */
public class MinaSshdManagementClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinaSshdManagementClient.class);

    private final ManagedChannel channel;

    private final String actorCrn;

    private final MinaSshdManagementClientConfig minaSshdManagementClientConfig;

    private final Tracer tracer;

    MinaSshdManagementClient(ManagedChannel channel, String actorCrn, MinaSshdManagementClientConfig minaSshdManagementClientConfig, Tracer tracer) {
        this.channel = checkNotNull(channel, "channel is null");
        this.actorCrn = checkNotNull(actorCrn, "actorCrn is null");
        this.minaSshdManagementClientConfig = checkNotNull(minaSshdManagementClientConfig);
        this.tracer = tracer;
    }

    /**
     * Wraps call to acquireMinaSshdService.
     *
     * @param requestId the request ID for the request
     * @param accountId the account ID
     * @return the minasshd service
     * @throws CcmException if an exception occurs
     */
    public MinaSshdService acquireMinaSshdService(String requestId, String accountId) throws CcmException {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(accountId, "accountId should not be null.");

        MinaSshdManagementBlockingStub blockingStub = newStub(requestId);
        AcquireMinaSshdServiceRequest.Builder requestBuilder = AcquireMinaSshdServiceRequest.newBuilder()
                .setAccountId(accountId);

        try {
            LOGGER.debug("Calling acquireMinaSshdService with requestId: {}, accountId: {}",
                    requestId, accountId);
            AcquireMinaSshdServiceResponse response = blockingStub.acquireMinaSshdService(requestBuilder.build());
            if (response == null) {
                throw new CcmException("Got null response from MinaSshdManagementService acquireMinaSshdService gRPC call", false);
            } else {
                MinaSshdService minaSshdService = response.getMinaSshdService();
                if (minaSshdService == null) {
                    throw new CcmException("Got null minasshd service in MinaSshdManagementService acquireMinaSshdService gRPC response", false);
                } else {
                    return minaSshdService;
                }
            }
        } catch (StatusRuntimeException e) {
            String message = "MinaSshdManagementService acquireMinaSshdService gRPC call failed: " + e.getMessage();
            Status status = e.getStatus();
            Status.Code code = status.getCode();
            boolean retryable = GrpcUtil.isRetryable(code);
            LOGGER.debug("Got status code: {}, retryable: {}", code, retryable);
            throw new CcmException(message, e, retryable);
        }
    }

    /**
     * Wraps calls to listMinaSshdServices with an account ID.
     *
     * @param requestId  the request ID for the request
     * @param accountId  the account ID
     * @param serviceIds the minasshd services to list. if null or empty then all minasshd services will be listed
     * @return the list of minasshd services
     */
    public List<MinaSshdService> listMinaSshdServices(String requestId, String accountId, List<String> serviceIds) throws CcmException {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(accountId, "accountId should not be null.");

        List<MinaSshdService> groups = new ArrayList<>();

        MinaSshdManagementBlockingStub minaSshdManagementBlockingStub = newStub(requestId);

        ListMinaSshdServicesRequest.Builder requestBuilder = ListMinaSshdServicesRequest.newBuilder()
                .setAccountId(accountId)
                .setPageSize(minaSshdManagementClientConfig.getListMinaSshdServicesPageSize());

        if (serviceIds != null && !serviceIds.isEmpty()) {
            requestBuilder.addAllId(serviceIds);
        }

        ListMinaSshdServicesResponse response;
        do {
            try {
                LOGGER.debug("Calling listMinaSshdServices with requestId: {}, accountId: {}, serviceIds: [{}]",
                        requestId, accountId, serviceIds);
                response = minaSshdManagementBlockingStub.listMinaSshdServices(requestBuilder.build());
                if (response == null) {
                    throw new CcmException("Got null response from MinaSshdManagementService listMinaSshdServices gRPC call", false);
                } else {
                    List<MinaSshdService> minaSshdServices = response.getMinaSshdServiceList();
                    if (minaSshdServices == null) {
                        throw new CcmException("Got null minasshd services in MinaSshdManagementService listMinaSshdServices gRPC response", false);
                    } else {
                        groups.addAll(minaSshdServices);
                    }
                }
            } catch (StatusRuntimeException e) {
                String message = "MinaSshdManagementService listMinaSshdServices gRPC call failed: " + e.getMessage();
                Status status = e.getStatus();
                Status.Code code = status.getCode();
                boolean retryable = GrpcUtil.isRetryable(code);
                LOGGER.debug("Got status code: {}, retryable: {}", code, retryable);
                throw new CcmException(message, e, retryable);
            }
            requestBuilder.setPageToken(response.getNextPageToken());
        } while (response.hasNextPageToken());

        return groups;
    }

    /**
     * Wraps call to generateAndRegisterSshTunnelingKeyPair.
     *
     * @param requestId         the request ID for the request
     * @param accountId         the account ID
     * @param minaSshdServiceId the minasshd service ID
     * @param keyId             the key ID
     * @return the response containing the key pair
     * @throws CcmException if an exception occurs
     */
    public GenerateAndRegisterSshTunnelingKeyPairResponse generateAndRegisterSshTunnelingKeyPair(
            String requestId, String accountId, String minaSshdServiceId, String keyId) throws CcmException {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(accountId, "accountId should not be null.");
        checkNotNull(minaSshdServiceId);
        checkNotNull(keyId);

        MinaSshdManagementBlockingStub blockingStub = newStub(requestId);
        GenerateAndRegisterSshTunnelingKeyPairRequest.Builder requestBuilder = GenerateAndRegisterSshTunnelingKeyPairRequest.newBuilder()
                .setAccountId(accountId)
                .setMinaSshdServiceId(minaSshdServiceId)
                .setKeyId(keyId);

        try {
            LOGGER.debug("Calling generateAndRegisterSshTunnelingKeyPair with requestId: {}, accountId: {}, minaSshdServiceId: {}, keyId: {}",
                    requestId, accountId, minaSshdServiceId, keyId);
            GenerateAndRegisterSshTunnelingKeyPairResponse response = blockingStub.generateAndRegisterSshTunnelingKeyPair(requestBuilder.build());
            if (response == null) {
                throw new CcmException("Got null response from MinaSshdManagementService generateAndRegisterSshTunnelingKeyPair gRPC call", false);
            } else {
                return response;
            }
        } catch (StatusRuntimeException e) {
            String message = "MinaSshdManagementService generateAndRegisterSshTunnelingKeyPair gRPC call failed: " + e.getMessage();
            Status status = e.getStatus();
            Status.Code code = status.getCode();
            boolean retryable = GrpcUtil.isRetryable(code);
            LOGGER.debug("Got status code: {}, retryable: {}", code, retryable);
            throw new CcmException(message, e, retryable);
        }
    }

    /**
     * Wraps call to unregisterSshTunnelingKey.
     *
     * @param requestId         the request ID for the request
     * @param minaSshdServiceId the minasshd service ID
     * @param keyId             the key ID
     * @return the response
     * @throws CcmException if an exception occurs
     */
    public UnregisterSshTunnelingKeyResponse unregisterSshTunnelingKey(
            String requestId, String minaSshdServiceId, String keyId) throws CcmException {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(minaSshdServiceId);
        checkNotNull(keyId);

        MinaSshdManagementBlockingStub blockingStub = newStub(requestId);
        UnregisterSshTunnelingKeyRequest.Builder requestBuilder = UnregisterSshTunnelingKeyRequest.newBuilder()
                .setMinaSshdServiceId(minaSshdServiceId)
                .setKeyId(keyId);

        try {
            LOGGER.debug("Calling unregisterSshTunnelingKey with requestId: {}, minaSshdServiceId: {}, keyId: {}",
                    requestId, minaSshdServiceId, keyId);
            UnregisterSshTunnelingKeyResponse response = blockingStub.unregisterSshTunnelingKey(requestBuilder.build());
            if (response == null) {
                throw new CcmException("Got null response from MinaSshdManagementService unregisterSshTunnelingKey gRPC call", false);
            } else {
                return response;
            }
        } catch (StatusRuntimeException e) {
            String message = "MinaSshdManagementService unregisterSshTunnelingKey gRPC call failed: " + e.getMessage();
            Status status = e.getStatus();
            Status.Code code = status.getCode();
            boolean retryable = GrpcUtil.isRetryable(code);
            LOGGER.debug("Got status code: {}, retryable: {}", code, retryable);
            throw new CcmException(message, e, retryable);
        }
    }

    /**
     * Creates a new stub with the appropriate metadata injecting interceptors.
     *
     * @param requestId the request ID
     * @return the stub
     */
    private MinaSshdManagementBlockingStub newStub(String requestId) {
        checkNotNull(requestId, "requestId should not be null.");
        return MinaSshdManagementGrpc.newBlockingStub(channel)
                .withInterceptors(GrpcUtil.getTracingInterceptor(tracer),
                        new AltusMetadataInterceptor(requestId, actorCrn));
    }
}
