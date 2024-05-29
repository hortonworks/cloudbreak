package com.sequenceiq.cloudbreak.auth.altus;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.auth.altus.config.UmsClientConfig;
import com.sequenceiq.cloudbreak.auth.altus.exception.UnauthorizedException;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class GrpcPersonalResourceViewClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcPersonalResourceViewClient.class);

    private final PersonalResourceViewClient personalResourceViewClient;

    GrpcPersonalResourceViewClient(ManagedChannel channel, UmsClientConfig umsClientConfig,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.personalResourceViewClient = new PersonalResourceViewClient(channel, umsClientConfig, regionAwareInternalCrnGeneratorFactory);
    }

    public List<Boolean> hasRightOnResources(String actorCrn, String right, Iterable<String> resources) {
        checkNotNull(actorCrn, "actorCrn should not be null.");
        checkNotNull(resources, "resources should not be null.");
        try {
            return personalResourceViewClient.hasResourcesByRight(actorCrn, right, resources);
        } catch (StatusRuntimeException statusRuntimeException) {
            Status.Code statusCode = statusRuntimeException.getStatus().getCode();
            if (Status.Code.DEADLINE_EXCEEDED.equals(statusCode)) {
                LOGGER.error("Deadline exceeded for hasRightOnResources for actor {} and right {} and resources {}", actorCrn, right, resources,
                        statusRuntimeException);
                throw new CloudbreakServiceException("Authorization failed due to user management service call timed out.");
            } else if (Status.Code.NOT_FOUND.equals(statusCode)) {
                LOGGER.error("NOT_FOUND error happened for hasRightOnResources {} for actor {} and resources {}! Cause: {}",
                        right, actorCrn, resources, statusRuntimeException.getMessage());
                throw new UnauthorizedException("Authorization failed for user: " + actorCrn);
            } else {
                LOGGER.error("Status runtime exception while checking hasRightOnResources for actor {} and right {} and resources {}", actorCrn, right,
                        resources, statusRuntimeException);
                throw new CloudbreakServiceException("Authorization failed due to user management service call failed.");
            }
        } catch (Exception e) {
            LOGGER.error("Unknown error while checking hasRightOnResources {} for actor {} and right {} and resources", actorCrn, right, resources, e);
            throw new CloudbreakServiceException("Authorization failed due to user management service call failed with error.");
        }
    }
}
