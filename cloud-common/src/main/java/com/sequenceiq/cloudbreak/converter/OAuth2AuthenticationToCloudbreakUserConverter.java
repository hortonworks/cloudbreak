package com.sequenceiq.cloudbreak.converter;

import javax.inject.Inject;

import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.client.CaasClient;
import com.sequenceiq.cloudbreak.client.CaasUser;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;

@Component
public class OAuth2AuthenticationToCloudbreakUserConverter extends AbstractConversionServiceAwareConverter<OAuth2Authentication, CloudbreakUser> {

    @Inject
    private CaasClient caasClient;

    @Override
    public CloudbreakUser convert(OAuth2Authentication source) {
        String username = (String) source.getPrincipal();
        String tenant = AuthenticatedUserService.getTenant(source);
        CaasUser userInfo = caasClient.getUserInfo(((OAuth2AuthenticationDetails) source.getDetails()).getTokenValue());
        return new CloudbreakUser(userInfo.getId(), username, userInfo.getEmail(), tenant);
    }
}
