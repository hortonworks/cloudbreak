package com.sequenceiq.cloudbreak.cloud.gcp.client.metric;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpRequest;

public class CompositeHttpExecuteInterceptor implements HttpExecuteInterceptor {

    private final List<HttpExecuteInterceptor> interceptors;

    public CompositeHttpExecuteInterceptor(List<HttpExecuteInterceptor> interceptors) {
        if (interceptors == null) {
            this.interceptors = new ArrayList<>();
        } else {
            this.interceptors = interceptors;
        }
    }

    @Override
    public void intercept(HttpRequest request) throws IOException {
        for (HttpExecuteInterceptor httpExecuteInterceptor : interceptors) {
            httpExecuteInterceptor.intercept(request);
        }
    }
}
