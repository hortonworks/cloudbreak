package com.sequenceiq.cloudbreak.auth.altus;


import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsConfig;

import io.grpc.ManagedChannelBuilder;

@Component
public class GrpcUmsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcUmsClient.class);

    @Inject
    private UmsConfig umsConfig;

    @Cacheable(cacheNames = "umsUserCache")
    public UserManagementProto.User getUserDetails(String actorCrn, String userCrn, String requestId) {
        try (ManagedChannelWrapper channelWrapper = new ManagedChannelWrapper(
                ManagedChannelBuilder.forAddress(umsConfig.getEndpoint(), umsConfig.getPort())
                        .usePlaintext()
                        .maxInboundMessageSize(DEFAULT_MAX_MESSAGE_SIZE)
                        .build())) {
            UmsClient client = new UmsClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.info("Getting user information for {} using request ID {}", userCrn, requestId);
            UserManagementProto.User user = client.getUser(requestId, userCrn);
            LOGGER.info("User information retrieved for userCrn: {}", user.getCrn());
            return user;
        }
    }

    public boolean isConfigured() {
        return umsConfig.isConfigured();
    }

    public boolean isUmsUsable(String crn) {
        return umsConfig.isConfigured() && Crn.isCrn(crn);
    }
}
