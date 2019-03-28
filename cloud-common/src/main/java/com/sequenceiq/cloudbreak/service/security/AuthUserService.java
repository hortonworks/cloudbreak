package com.sequenceiq.cloudbreak.service.security;

import java.util.UUID;

import javax.inject.Inject;

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

    @Inject
    private CaasClient caasClient;

    @Inject
    private GrpcUmsClient umsClient;

    public CloudbreakUser getUserWithCaasFallback(OAuth2Authentication auth) {
        CloudbreakUser user;
        String token = ((OAuth2AuthenticationDetails) auth.getDetails()).getTokenValue();
        if (umsClient.isUmsUsable(token)) {
            user = createCbUserWithUms(auth);
        } else {
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
        return new CloudbreakUser(userInfo.getId(), username, userInfo.getEmail(), tenant, null);
    }

    private CloudbreakUser createCbUserWithUms(OAuth2Authentication auth) {
        String userCrn = ((OAuth2AuthenticationDetails) auth.getDetails()).getTokenValue();
        String requestId = MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString());
        UserManagementProto.User userInfo = umsClient.getUserDetails(userCrn, userCrn, requestId != null ? requestId : UUID.randomUUID().toString());
        return new CloudbreakUser(userInfo.getUserId(), (String) auth.getPrincipal(), userInfo.getEmail(), Crn.fromString(userCrn).getAccountId(), userCrn);
    }
}
