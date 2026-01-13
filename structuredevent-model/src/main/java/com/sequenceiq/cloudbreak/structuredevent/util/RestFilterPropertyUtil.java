package com.sequenceiq.cloudbreak.structuredevent.util;

import static com.sequenceiq.cloudbreak.common.request.HeaderConstants.REQUEST_ID_HEADER;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestRequestDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestResponseDetails;
import com.sequenceiq.cloudbreak.structuredevent.filter.CDPJaxRsFilterPropertyKeys;

public class RestFilterPropertyUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestFilterPropertyUtil.class);

    private static final List<String> SKIPPED_HEADERS_LIST = Lists.newArrayList("authorization");

    private RestFilterPropertyUtil() {
    }

    public static RestRequestDetails createRequestDetails(ContainerRequestContext requestContext, String body) {
        LOGGER.debug("Request body length: {}", body.length());
        RestRequestDetails restRequest = new RestRequestDetails();
        restRequest.setRequestUri(requestContext.getUriInfo().getRequestUri().toString());
        restRequest.setBody(body);
        restRequest.setCookies(convertCookies(requestContext));
        restRequest.setHeaders(convertHeaders(requestContext));
        restRequest.setMediaType(getMediaType(requestContext.getMediaType()));
        restRequest.setMethod(requestContext.getMethod());
        String requestId = requestContext.getHeaderString(REQUEST_ID_HEADER);
        restRequest.setRequestId(requestId);
        return restRequest;
    }

    public static RestResponseDetails createResponseDetails(ContainerResponseContext responseContext) {
        RestResponseDetails restResponse = new RestResponseDetails();
        restResponse.setStatusCode(responseContext.getStatus());
        Response.StatusType responseStatus = responseContext.getStatusInfo();
        restResponse.setStatusText(responseStatus.toEnum() != null ? responseStatus.toEnum().name() : responseStatus.getReasonPhrase());
        restResponse.setCookies(responseContext.getCookies().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString())));
        restResponse.setHeaders(responseContext.getHeaders().entrySet().stream().filter(e -> !SKIPPED_HEADERS_LIST.contains(e.getKey())).collect(
                Collectors.toMap(Map.Entry::getKey, e -> StringUtils.join(e.getValue(), ","))));
        restResponse.setMediaType(getMediaType(responseContext.getMediaType()));
        return restResponse;
    }

    public static void setPropertyIfAbsent(ContainerRequestContext requestContext, String key, Object value) {
        if (Objects.isNull(requestContext.getProperty(key))) {
            requestContext.setProperty(key, value);
        }
    }

    public static void extractAndSetResponseEntityStreamIfAbsent(ContainerRequestContext requestContext, ContainerResponseContext responseContext,
            Boolean contentLogging) {
        if (Objects.isNull(requestContext.getProperty(CDPJaxRsFilterPropertyKeys.RESPONSE_LOGGING_STREAM))) {
            OutputStream stream = new LoggingStream(responseContext.getEntityStream(), contentLogging);
            responseContext.setEntityStream(stream);
            requestContext.setProperty(CDPJaxRsFilterPropertyKeys.RESPONSE_LOGGING_STREAM, stream);
        }
    }

    private static Map<String, String> convertHeaders(ContainerRequestContext requestContext) {
        return requestContext.getHeaders().entrySet().stream().filter(e -> !SKIPPED_HEADERS_LIST.contains(e.getKey())).collect(
                Collectors.toMap(Map.Entry::getKey, e -> StringUtils.join(e.getValue(), ",")));
    }

    private static Map<String, String> convertCookies(ContainerRequestContext requestContext) {
        return requestContext.getCookies().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
    }

    private static String getMediaType(MediaType mediaType) {
        return mediaType != null ? mediaType.toString() : "";
    }
}
