package com.sequenceiq.cloudbreak.cloud.gcp.client;

import javax.inject.Inject;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpRequestInitializer;
import com.sequenceiq.cloudbreak.cloud.gcp.tracing.GcpTracingInterceptor;

public abstract class GcpServiceFactory {

    @Inject
    private GcpTracingInterceptor gcpTracingInterceptor;

    protected HttpRequestInitializer requestInitializer(GoogleCredential credential) {
        return request -> {
            credential.initialize(request);
            request.setInterceptor(requestInterceptor(credential));
            request.setResponseInterceptor(gcpTracingInterceptor);
        };
    }

    private HttpExecuteInterceptor requestInterceptor(GoogleCredential credential) {
        return request -> {
            credential.intercept(request);
            gcpTracingInterceptor.intercept(request);
        };
    }
}
