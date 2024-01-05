package com.sequenceiq.cloudbreak.service.secret.conf;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.hc.client5.http.RedirectException;
import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.springframework.stereotype.Component;

@Component
public class LaxRedirectStrategy extends DefaultRedirectStrategy {

    private static final String[] REDIRECT_METHODS = { "GET", "POST", "HEAD", "PUT", "DELETE" };

    @Override
    public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) throws RedirectException {
        if (response == null) {
            throw new IllegalArgumentException("HTTP response may not be null");
        }
        int statusCode = response.getCode();
        String method = request.getMethod();
        return statusCode >= HttpStatus.SC_MOVED_PERMANENTLY && statusCode <= HttpStatus.SC_PERMANENT_REDIRECT && ArrayUtils.contains(REDIRECT_METHODS, method);
    }

}
