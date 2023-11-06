package com.sequenceiq.cloudbreak.cloud.gcp.client;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpBackOffUnsuccessfulResponseHandler;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpUnsuccessfulResponseHandler;
import com.google.api.client.util.ExponentialBackOff;

public class GcpCustomHttpUnsuccessfulResponseHandler implements HttpUnsuccessfulResponseHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpCustomHttpUnsuccessfulResponseHandler.class);

    private final GoogleCredential credential;

    private final HttpBackOffUnsuccessfulResponseHandler httpBackOffUnsuccessfulRespHandler;

    GcpCustomHttpUnsuccessfulResponseHandler(GoogleCredential credential, ExponentialBackOff backOff) {
        this.credential = credential;
        this.httpBackOffUnsuccessfulRespHandler = new HttpBackOffUnsuccessfulResponseHandler(backOff);
    }

    @Override
    public boolean handleResponse(HttpRequest request, HttpResponse response, boolean supportsRetry) throws IOException {
        LOGGER.debug("Handling unsuccessful response for request method '{}', URL '{}', response status: '{}', supportsRetry: '{}'",
                request.getRequestMethod(), request.getUrl(), response.getStatusCode(), supportsRetry);
        boolean handled = false;
        if (credential.handleResponse(request, response, supportsRetry)) {
            LOGGER.debug("Credential handled the response token refresh was needed.");
            handled = true;
        }
        if (!handled) {
            LOGGER.trace("Propagate the handle of unsuccessful response to HttpBackOffUnsuccessfulResponseHandler");
            handled = httpBackOffUnsuccessfulRespHandler.handleResponse(request, response, supportsRetry);
        }
        return handled;
    }
}
