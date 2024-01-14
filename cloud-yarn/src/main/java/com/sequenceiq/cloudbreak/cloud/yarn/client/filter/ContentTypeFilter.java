package com.sequenceiq.cloudbreak.cloud.yarn.client.filter;

import java.io.IOException;
import java.util.List;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.apache.commons.collections4.CollectionUtils;

/**
 * ContentTypeFilter removes multiple content types if the content type header
 * contains "application/json" and it replaces that with
 * a single-element Content-Type header containing "application/json" only
 *
 * WARNING: this filter must be set with the lowest priority.
 * Because it is a response filter it means its priority value must be the highest possible number,
 * i.e. Integer.MAX_VALUE
 */
public class ContentTypeFilter implements ClientResponseFilter {

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        // Remove duplicate Content-Type headers and force application/json on the response
        List<String> contentTypeHeaders = responseContext.getHeaders().get(HttpHeaders.CONTENT_TYPE);
        if (CollectionUtils.isNotEmpty(contentTypeHeaders)
                && contentTypeHeaders.contains(MediaType.APPLICATION_JSON)
                && contentTypeHeaders.size() > 1) {
            responseContext.getHeaders().remove(HttpHeaders.CONTENT_TYPE);
            responseContext.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        }
    }

}
