package com.sequenceiq.freeipa.client;

import static com.sequenceiq.cloudbreak.client.RequestIdProvider.REQUEST_ID_HEADER;

import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

import com.sequenceiq.cloudbreak.client.RequestIdProvider;

public class RequestIdProviderInterceptor implements HttpRequestInterceptor {

    @Override
    public void process(HttpRequest request, HttpContext context) {
        request.addHeader(REQUEST_ID_HEADER, RequestIdProvider.getOrGenerateRequestId());
    }
}
