package com.sequenceiq.cloudbreak.service.security;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.client.CaasClient;
import com.sequenceiq.cloudbreak.client.CaasUser;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@Component
public class AuthUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthUserService.class);

    @Inject
    private CaasClient caasClient;

    @Inject
    private GrpcUmsClient umsClient;

    public CloudbreakUser getUserWithCaasFallback(OAuth2Authentication auth) {
        CloudbreakUser user;
        String token = ((OAuth2AuthenticationDetails) auth.getDetails()).getTokenValue();
        if (umsClient.isUmsUsable(token)) {
            LOGGER.debug("Using UMS.");
            user = createCbUserWithUms(auth);
        } else {
            LOGGER.debug("Falling back on CAAS.");
            user = createCbUserWithCaas(auth);
        }
        return user;
    }

    public String getUserCrn(OAuth2Authentication auth) throws CloudbreakException {
        if (umsClient.isConfigured()) {
            return ((OAuth2AuthenticationDetails) auth.getDetails()).getTokenValue();
        }
        throw new CloudbreakException("UMS Client is not configured, userCRN cannot be retrieved.");
    }

    private CloudbreakUser createCbUserWithCaas(OAuth2Authentication auth) {
        String username = (String) auth.getPrincipal();
        String tenant = AuthenticatedUserService.getTenant(auth);
        CaasUser userInfo = caasClient.getUserInfo(((OAuth2AuthenticationDetails) auth.getDetails()).getTokenValue());
        return new CloudbreakUser(userInfo.getId(), null, username, userInfo.getEmail(), tenant);
    }

    private CloudbreakUser createCbUserWithUms(OAuth2Authentication auth) {
        String userCrn = ((OAuth2AuthenticationDetails) auth.getDetails()).getTokenValue();
        String requestId = MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString());
        UserManagementProto.User userInfo = umsClient.getUserDetails(userCrn, userCrn, Optional.ofNullable(requestId));
        return new CloudbreakUser(userInfo.getUserId(), userCrn,
                (String) auth.getPrincipal(), userInfo.getEmail(), Crn.fromString(userCrn).getAccountId());
    }
}
