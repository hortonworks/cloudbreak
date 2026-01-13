package com.sequenceiq.freeipa.client;

import static com.sequenceiq.cloudbreak.common.request.HeaderConstants.REQUEST_ID_HEADER;

import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;

public class RequestIdProviderInterceptor implements HttpRequestInterceptor {

    @Override
    public void process(HttpRequest request, HttpContext context) {
        request.addHeader(REQUEST_ID_HEADER, MDCBuilder.getOrGenerateRequestId());
    }
}
