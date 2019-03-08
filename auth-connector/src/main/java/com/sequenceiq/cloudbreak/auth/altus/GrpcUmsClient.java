package com.sequenceiq.cloudbreak.auth.altus;


import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;
import static java.lang.String.format;

import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsConfig;

import io.grpc.ManagedChannelBuilder;

@Component
public class GrpcUmsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcUmsClient.class);

    @Inject
    private UmsConfig umsConfig;

    private static String newRequestId() {
        return UUID.randomUUID().toString();
    }

    public UserManagementProto.User getUserDetails(String actorCrn, String userCrn) {
        try (ManagedChannelWrapper channelWrapper = new ManagedChannelWrapper(
                ManagedChannelBuilder.forAddress(umsConfig.getEndpoint(), umsConfig.getPort())
                        .usePlaintext()
                        .maxInboundMessageSize(DEFAULT_MAX_MESSAGE_SIZE)
                        .build())) {
            UmsClient client = new UmsClient(channelWrapper.getChannel(), actorCrn);
            String requestId = newRequestId();
            LOGGER.info(format("Getting user information for %s using request ID %s", userCrn, requestId));
            UserManagementProto.User user = client.getUser(requestId, userCrn);
            LOGGER.info("User information:");
            LOGGER.info(user.toString());
            return user;
        }
    }

    public boolean isConfigured() {
        return umsConfig.isConfigured();
    }
}
