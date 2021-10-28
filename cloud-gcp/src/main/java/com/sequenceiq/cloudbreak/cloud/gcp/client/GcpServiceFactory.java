package com.sequenceiq.cloudbreak.cloud.gcp.client;

import javax.inject.Inject;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpBackOffIOExceptionHandler;
import com.google.api.client.http.HttpBackOffUnsuccessfulResponseHandler;
import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpUnsuccessfulResponseHandler;
import com.google.api.client.util.ExponentialBackOff;
import com.sequenceiq.cloudbreak.cloud.gcp.tracing.GcpTracingInterceptor;

public abstract class GcpServiceFactory {

    @Inject
    private GcpTracingInterceptor gcpTracingInterceptor;

    protected HttpRequestInitializer requestInitializer(GoogleCredential credential) {
        ExponentialBackOff backOff = new ExponentialBackOff();
        return request -> {
            credential.initialize(request);
            request.setInterceptor(requestInterceptor(credential));
            request.setResponseInterceptor(gcpTracingInterceptor);
            request.setIOExceptionHandler(new HttpBackOffIOExceptionHandler(backOff));
            request.setUnsuccessfulResponseHandler(unsuccessfulResponseHandler(credential, backOff));
        };
    }

    private HttpExecuteInterceptor requestInterceptor(GoogleCredential credential) {
        return request -> {
            credential.intercept(request);
            gcpTracingInterceptor.intercept(request);
        };
    }

    private HttpUnsuccessfulResponseHandler unsuccessfulResponseHandler(GoogleCredential credential, ExponentialBackOff backOff) {
        HttpBackOffUnsuccessfulResponseHandler httpBackOffUnsuccessfulRespHandler = new HttpBackOffUnsuccessfulResponseHandler(backOff);
        return (request, response, supportsRetry) ->
                credential.handleResponse(request, response, supportsRetry)
                        || httpBackOffUnsuccessfulRespHandler.handleResponse(request, response, supportsRetry);
    }
}
