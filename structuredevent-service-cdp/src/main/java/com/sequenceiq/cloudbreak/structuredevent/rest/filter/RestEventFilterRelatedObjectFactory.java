package com.sequenceiq.cloudbreak.structuredevent.rest.filter;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.REST;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser.RESOURCE_CRN;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser.RESOURCE_EVENT;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser.RESOURCE_ID;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser.RESOURCE_NAME;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser.RESOURCE_TYPE;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestRequestDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestResponseDetails;
import com.sequenceiq.flow.ha.NodeConfig;

@Component
class RestEventFilterRelatedObjectFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestEventFilterRelatedObjectFactory.class);

    private final List<String> skippedHeadersList = Lists.newArrayList("authorization");

    @Value("${info.app.version:}")
    private String cbVersion;

    @Inject
    private NodeConfig nodeConfig;

    public CDPOperationDetails createCDPOperationDetails(Map<String, String> restParams, Long requestTime) {
        String resourceType = null;
        String resourceId = null;
        String resourceName = null;
        String resourceCrn = null;
        String resourceEvent = null;
        if (restParams != null) {
            resourceType = restParams.get(RESOURCE_TYPE);
            resourceId = restParams.get(RESOURCE_ID);
            resourceName = restParams.get(RESOURCE_NAME);
            resourceCrn = restParams.get(RESOURCE_CRN);
            resourceEvent = restParams.get(RESOURCE_EVENT);
        }
        return new CDPOperationDetails(requestTime,
                REST,
                resourceType,
                StringUtils.isNotEmpty(resourceId) ? Long.valueOf(resourceId) : null,
                resourceName,
                nodeConfig.getId(),
                cbVersion,
                ThreadBasedUserCrnProvider.getAccountId(),
                resourceCrn,
                ThreadBasedUserCrnProvider.getUserCrn(),
                null,
                resourceEvent);

    }

    public RestRequestDetails createRequestDetails(ContainerRequestContext requestContext, String body) {
        LOGGER.debug("Request body length: {}", body.length());
        RestRequestDetails restRequest = new RestRequestDetails();
        restRequest.setRequestUri(requestContext.getUriInfo().getRequestUri().toString());
        restRequest.setBody(body);
        restRequest.setCookies(convertCookies(requestContext));
        restRequest.setHeaders(convertHeaders(requestContext));
        restRequest.setMediaType(getMediaType(requestContext.getMediaType()));
        restRequest.setMethod(requestContext.getMethod());
        return restRequest;
    }

    private Map<String, String> convertHeaders(ContainerRequestContext requestContext) {
        return requestContext.getHeaders().entrySet().stream().filter(e -> !skippedHeadersList.contains(e.getKey())).collect(
                Collectors.toMap(Map.Entry::getKey, e -> StringUtils.join(e.getValue(), ",")));
    }

    private Map<String, String> convertCookies(ContainerRequestContext requestContext) {
        return requestContext.getCookies().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
    }

    public RestResponseDetails createResponseDetails(ContainerResponseContext responseContext) {
        RestResponseDetails restResponse = new RestResponseDetails();
        restResponse.setStatusCode(responseContext.getStatus());
        restResponse.setStatusText(responseContext.getStatusInfo().toEnum().name());
        restResponse.setCookies(responseContext.getCookies().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString())));
        restResponse.setHeaders(responseContext.getHeaders().entrySet().stream().filter(e -> !skippedHeadersList.contains(e.getKey())).collect(
                Collectors.toMap(Map.Entry::getKey, e -> StringUtils.join(e.getValue(), ","))));
        restResponse.setMediaType(getMediaType(responseContext.getMediaType()));
        return restResponse;
    }

    private String getMediaType(MediaType mediaType) {
        return mediaType != null ? mediaType.toString() : "";
    }
}
