package com.sequenceiq.cloudbreak.auth.altus;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.authentication.BearerTokenExtractor;
import org.springframework.security.oauth2.provider.authentication.TokenExtractor;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import io.netty.util.internal.StringUtil;

public class CrnTokenExtractor implements TokenExtractor {

    private static final String CRN_HEADER = "x-cdp-actor-crn";

    @Override
    public Authentication extract(HttpServletRequest request) {
        if (StringUtil.isNullOrEmpty(request.getHeader(CRN_HEADER))) {
            return new BearerTokenExtractor().extract(request);
        }
        String tokenValue = extractHeaderToken(request, CRN_HEADER);
        return new PreAuthenticatedAuthenticationToken(tokenValue, "");
    }

    protected String extractHeaderToken(HttpServletRequest request, String headerName) {
        return request.getHeader(headerName);
    }
}
