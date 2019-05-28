package com.sequenceiq.cloudbreak.auth.security.token;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.authentication.TokenExtractor;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import com.sequenceiq.cloudbreak.auth.security.authentication.DisabledAuthCbUserProvider;

public class DisabledAuthTokenExtractor implements TokenExtractor {

    private final DisabledAuthCbUserProvider disabledAuthCbUserProvider;

    public DisabledAuthTokenExtractor(DisabledAuthCbUserProvider disabledAuthCbUserProvider) {
        this.disabledAuthCbUserProvider = disabledAuthCbUserProvider;
    }

    @Override
    public Authentication extract(HttpServletRequest request) {
        return new PreAuthenticatedAuthenticationToken(disabledAuthCbUserProvider.getCloudbreakUser(), "");
    }
}
