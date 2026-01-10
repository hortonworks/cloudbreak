package com.sequenceiq.cloudbreak.clusterproxy;

import static com.sequenceiq.cloudbreak.common.request.HeaderConstants.REQUEST_ID_HEADER;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;

public class RequestIdProviderRequestInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        HttpHeaders headers = request.getHeaders();
        if (!headers.containsKey(REQUEST_ID_HEADER)) {
            headers.add(REQUEST_ID_HEADER, MDCBuilder.getOrGenerateRequestId());
        }
        return execution.execute(request, body);
    }
}
