package com.sequenceiq.cloudbreak.authentication;

import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;

import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.auth.caas.CaasClient;
import com.sequenceiq.cloudbreak.auth.caas.CaasUser;

public class CaasAuthenticationService implements AuthenticationService {

    private final CaasClient caasClient;

    public CaasAuthenticationService(CaasClient caasClient) {
        this.caasClient = caasClient;
    }

    @Override
    public CloudbreakUser getCloudbreakUser(OAuth2Authentication auth) {
        String username = (String) auth.getPrincipal();
        String tenant = AuthenticatedUserService.getTenant(auth);
        CaasUser userInfo = caasClient.getUserInfo(((OAuth2AuthenticationDetails) auth.getDetails()).getTokenValue());
        return new CloudbreakUser(userInfo.getId(), null, username, userInfo.getEmail(), tenant);
    }
}
