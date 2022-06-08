package com.sequenceiq.cloudbreak.auth.altus;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewGrpc;
import com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsClientConfig;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.grpc.altus.CallingServiceNameInterceptor;
import com.sequenceiq.cloudbreak.grpc.util.GrpcUtil;
import com.sequenceiq.cloudbreak.logger.MDCUtils;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.opentracing.Tracer;

/**
 * A simple wrapper to the GRPC user management service. This handles setting up
 * the appropriate context-propogatinng interceptors and hides some boilerplate.
 */
public class PersonalResourceViewClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersonalResourceViewClient.class);

    private final ManagedChannel channel;

    private final String actorCrn;

    private final UmsClientConfig umsClientConfig;

    private final Tracer tracer;

    /**
     * Constructor.
     *
     * @param channel  the managed channel.
     * @param actorCrn the actor CRN.
     * @param tracer   tracer
     */
    PersonalResourceViewClient(ManagedChannel channel, String actorCrn, UmsClientConfig umsClientConfig, Tracer tracer) {
        this.channel = checkNotNull(channel, "channel should not be null.");
        this.actorCrn = checkNotNull(actorCrn, "actorCrn should not be null.");
        this.umsClientConfig = checkNotNull(umsClientConfig, "umsClientConfig should not be null.");
        this.tracer = tracer;
    }

    public List<Boolean> hasRightOnResources(String actorCrn, String right, Iterable<String> resources) {
        checkNotNull(actorCrn, "actorCrn should not be null.");
        checkNotNull(resources, "resources should not be null.");
        try {
            return newStub()
                    .hasResourcesByRight(
                            PersonalResourceViewProto.HasResourcesByRightRequest
                                    .newBuilder()
                                    .setUserCrn(actorCrn)
                                    .setRight(right)
                                    .addAllResource(resources)
                                    .build())
                    .getResultList();
        } catch (StatusRuntimeException statusRuntimeException) {
            if (Status.Code.DEADLINE_EXCEEDED.equals(statusRuntimeException.getStatus().getCode())) {
                LOGGER.error("Deadline exceeded for hasRightOnResources {} for actor {} and right {} and resources {}", actorCrn, right, resources,
                        statusRuntimeException);
                throw new CloudbreakServiceException("Authorization failed due to user management service call timed out.");
            } else {
                LOGGER.error("Status runtime exception while checking hasRightOnResources {} for actor {} and right {} and resources {}", actorCrn, right,
                        resources, statusRuntimeException);
                throw new CloudbreakServiceException("Authorization failed due to user management service call failed.");
            }
        } catch (Exception e) {
            LOGGER.error("Unknown error while checking hasRightOnResources {} for actor {} and right {} and resources", actorCrn, right, resources, e);
            throw new CloudbreakServiceException("Authorization failed due to user management service call failed with error.");
        }
    }

    /**
     * Creates a new stub with the appropriate metadata injecting interceptors.
     *
     * @param requestId the request ID
     * @return the stub
     */
    private PersonalResourceViewGrpc.PersonalResourceViewBlockingStub newStub() {
        String requestId = RequestIdUtil.getOrGenerate(MDCUtils.getRequestId());
        return PersonalResourceViewGrpc.newBlockingStub(channel).withInterceptors(
                GrpcUtil.getTimeoutInterceptor(umsClientConfig.getGrpcShortTimeoutSec()),
                GrpcUtil.getTracingInterceptor(tracer),
                new AltusMetadataInterceptor(requestId, actorCrn),
                new CallingServiceNameInterceptor(umsClientConfig.getCallingServiceName())
        );
    }
}
