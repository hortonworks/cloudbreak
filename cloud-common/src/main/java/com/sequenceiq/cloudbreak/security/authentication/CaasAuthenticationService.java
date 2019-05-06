package com.sequenceiq.cloudbreak.security.authentication;

import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;

import com.sequenceiq.cloudbreak.client.CaasClient;
import com.sequenceiq.cloudbreak.client.CaasUser;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;

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
