package com.sequenceiq.cloudbreak.service.security;

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
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;

@Component
public class AuthUserService {

    private static final String CRN_HEADER = "x-cdp-actor-crn";

    @Inject
    private CaasClient caasClient;

    @Inject
    private GrpcUmsClient umsClient;

    public CloudbreakUser getUserWithCaasFallback(OAuth2Authentication auth) {
        CloudbreakUser user;
        if (umsClient.isConfigured()) {
            user = createCbUserWithUms(auth);
        } else {
            user = createCbUserWithCaas(auth);
        }
        return user;
    }

    private CloudbreakUser createCbUserWithCaas(OAuth2Authentication auth) {
        String username = (String) auth.getPrincipal();
        String tenant = AuthenticatedUserService.getTenant(auth);
        CaasUser userInfo = caasClient.getUserInfo(((OAuth2AuthenticationDetails) auth.getDetails()).getTokenValue());
        return new CloudbreakUser(userInfo.getId(), username, userInfo.getEmail(), tenant);
    }

    private CloudbreakUser createCbUserWithUms(OAuth2Authentication auth) {
        String userCrn = ((OAuth2AuthenticationDetails) auth.getDetails()).getTokenValue();
        UserManagementProto.User userInfo = umsClient.getUserDetails(userCrn, userCrn);
        return new CloudbreakUser(userInfo.getUserId(), (String) auth.getPrincipal(), userInfo.getEmail(), Crn.getAccountId(userCrn));
    }

}
