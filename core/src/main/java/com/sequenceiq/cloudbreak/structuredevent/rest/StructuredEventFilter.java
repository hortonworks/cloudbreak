package com.sequenceiq.cloudbreak.structuredevent.rest;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.REST;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.message.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.ha.CloudbreakNodeConfig;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.structuredevent.StructuredEventClient;
import com.sequenceiq.cloudbreak.structuredevent.event.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestCallDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestRequestDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestResponseDetails;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class StructuredEventFilter implements WriterInterceptor, ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredEventFilter.class);

    private static final String LOGGING_ENABLED_PROPERTY = "structuredevent.loggingEnabled";

    private static final String LOGGINGSTREAM_PROPERTY = "structuredevent.entityLogger";

    private static final String REST_PARAMS = "REST_PARAMS";

    private static final String REQUEST_TIME = "REQUEST_TIME";

    private static final String REQUEST_DETAILS = "REQUEST_DETAIS";

    private static final String RESPONSE_DETAILS = "RESPONSE_DETAIS";

    private static final int MAX_CONTENT_LENGTH = 65535;

    private static final String RESOURCE_TYPE = "RESOURCE_TYPE";

    private static final String RESOURCE_ID = "RESOURCE_ID";

    private static final String RESOURCE_NAME = "RESOURCE_NAME";

    private final List<String> urlBlackList = Lists.newArrayList("connectors", "flexsubscriptions", "accountpreferences", "users");

    private final List<String> skippedHeadersList = Lists.newArrayList("authorization");

    private final List<String> postUrlPatterns = Lists.newArrayList("\\/cb\\/api\\/v3\\/\\d+\\/([a-z]*).*", "\\/cb\\/api\\/v.\\/([a-z]*).*");

    private final List<String> deleteUrlPatterns = Lists.newArrayList(
            "\\/cb\\/api\\/v.\\/([a-z]*)\\/([0-9]+)",
            "\\/cb\\/api\\/v.\\/([a-z]*)\\/(user|account)\\/(.*)");

    private final List<String> putUrlPatterns = Lists.newArrayList(
            "\\/cb\\/api\\/v.\\/([a-z]*)\\/([0-9]+).*", "\\/cb\\/api\\/v.\\/([a-z]*).*");

    @Inject
    private CloudbreakNodeConfig cloudbreakNodeConfig;

    @Value("${info.app.version:}")
    private String cbVersion;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    @Named("structuredEventClient")
    private StructuredEventClient structuredEventClient;

    @Value("${cb.structuredevent.rest.contentlogging:false}")
    private Boolean contentLogging;

    private final Pattern extendRestParamsFromResponsePattern = Pattern.compile("\"id\":([0-9]*)");

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        boolean loggingEnabled = isLoggingEnabled(requestContext);
        requestContext.setProperty(LOGGING_ENABLED_PROPERTY, loggingEnabled);
        if (loggingEnabled) {
            requestContext.setProperty(REQUEST_TIME, System.currentTimeMillis());
            StringBuilder body = new StringBuilder();
            requestContext.setEntityStream(logInboundEntity(body, requestContext.getEntityStream(), MessageUtils.getCharset(requestContext.getMediaType())));
            requestContext.setProperty(REST_PARAMS,
                    getRequestUrlParameters(requestContext.getMethod(), requestContext.getUriInfo().getRequestUri().getPath()));
            requestContext.setProperty(REQUEST_DETAILS, createRequestDetails(requestContext, body.toString()));
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        if (BooleanUtils.isTrue((Boolean) requestContext.getProperty(LOGGING_ENABLED_PROPERTY))) {
            RestResponseDetails restResponse = createResponseDetails(responseContext);
            if (responseContext.hasEntity()) {
                OutputStream stream = new LoggingStream(responseContext.getEntityStream());
                responseContext.setEntityStream(stream);
                requestContext.setProperty(LOGGINGSTREAM_PROPERTY, stream);
                requestContext.setProperty(RESPONSE_DETAILS, restResponse);
            } else {
                Long requestTime = (Long) requestContext.getProperty(REQUEST_TIME);
                RestRequestDetails restRequest = (RestRequestDetails) requestContext.getProperty(REQUEST_DETAILS);
                Map<String, String> restParams = (Map<String, String>) requestContext.getProperty(REST_PARAMS);
                sendStructuredEvent(restRequest, restResponse, restParams, requestTime, "");
            }
        }
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        context.proceed();
        if (BooleanUtils.isTrue((Boolean) context.getProperty(LOGGING_ENABLED_PROPERTY))) {
            Long requestTime = (Long) context.getProperty(REQUEST_TIME);
            RestRequestDetails restRequest = (RestRequestDetails) context.getProperty(REQUEST_DETAILS);
            RestResponseDetails restResponse = (RestResponseDetails) context.getProperty(RESPONSE_DETAILS);
            String responseBody = ((LoggingStream) context.getProperty(LOGGINGSTREAM_PROPERTY)).getStringBuilder(
                    MessageUtils.getCharset(context.getMediaType())).toString();
            Map<String, String> restParams = (Map<String, String>) context.getProperty(REST_PARAMS);
            extendRestParamsFromResponse(restParams, responseBody);
            sendStructuredEvent(restRequest, restResponse, restParams, requestTime, responseBody);
        }
    }

    private void sendStructuredEvent(RestRequestDetails restRequest, RestResponseDetails restResponse, Map<String, String> restParams, Long requestTime,
            String responseBody) {
        restResponse.setBody(responseBody);
        RestCallDetails restCall = new RestCallDetails();
        restCall.setRestRequest(restRequest);
        restCall.setRestResponse(restResponse);
        restCall.setDuration(System.currentTimeMillis() - requestTime);
        Long orgId = restRequestThreadLocalService.getRequestedOrgId();
        if (orgId == null) {
            orgId = organizationService.getDefaultOrganizationForCurrentUser().getId();
        }
        User user = userService.getCurrentUser();
        structuredEventClient.sendStructuredEvent(new StructuredRestCallEvent(createOperationDetails(restParams, requestTime, orgId),
                restCall, orgId, user.getUserId()));
    }

    private Map<String, String> getRequestUrlParameters(String method, CharSequence url) {
        Map<String, String> params = Maps.newHashMap();
        List<String> urlPatterns;
        switch (method) {
            case "POST":
                urlPatterns = postUrlPatterns;
                break;
            case "DELETE":
                urlPatterns = deleteUrlPatterns;
                break;
            case "PUT":
                urlPatterns = putUrlPatterns;
                break;
            default:
                urlPatterns = Lists.newArrayList();
        }
        for (String urlRegex : urlPatterns) {
            Pattern pattern = Pattern.compile(urlRegex);
            Matcher matcher = pattern.matcher(url);
            if (matcher.matches()) {
                if (matcher.groupCount() >= 1) {
                    params.put(RESOURCE_TYPE, matcher.group(1));
                }
                if (matcher.groupCount() >= 2) {
                    String identifier = matcher.group(2);
                    if (StringUtils.isNumeric(identifier)) {
                        params.put(RESOURCE_ID, identifier);
                    } else {
                        params.put(RESOURCE_NAME, identifier);
                    }
                }
                break;
            }
        }
        return params;
    }

    private void extendRestParamsFromResponse(Map<String, String> params, CharSequence responseBody) {
        if (responseBody != null && !params.containsKey(RESOURCE_ID)) {
            String resourceId = null;
            try {
                JsonNode jsonNode = JsonUtil.readTree(responseBody.toString());
                JsonNode idNode = jsonNode.path("id");
                resourceId = idNode.asText();
            } catch (IOException e) {
                LOGGER.warn("Parsing of ID from JSON response failed", e);
            }
            if (StringUtils.isEmpty(resourceId)) {
                Matcher matcher = extendRestParamsFromResponsePattern.matcher(responseBody);
                if (matcher.find() && matcher.groupCount() >= 1) {
                    resourceId = matcher.group(1);
                }
            }

            if (resourceId != null) {
                params.put(RESOURCE_ID, resourceId);
            }
        }
    }

    private boolean isLoggingEnabled(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getRequestUri().getPath();
        return !"GET".equals(requestContext.getMethod()) && urlBlackList.stream().noneMatch(path::contains);
    }

    private OperationDetails createOperationDetails(Map<String, String> restParams, Long requestTime, Long orgId) {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        User currentUser = userService.getCurrentUser();
        String resoureceType = restParams.get(RESOURCE_TYPE);
        String resoureceId = restParams.get(RESOURCE_ID);
        String resoureceName = restParams.get(RESOURCE_NAME);
        return new OperationDetails(requestTime, REST, resoureceType, StringUtils.isNotEmpty(resoureceId) ? Long.valueOf(resoureceId) : null, resoureceName,
                currentUser != null ? currentUser.getUserId() : "", currentUser != null ? currentUser.getUserName() : "",
                cloudbreakNodeConfig.getId(), cbVersion, orgId, identityUser != null ? identityUser.getAccount() : "",
                identityUser != null ? identityUser.getUserId() : "", identityUser != null ? identityUser.getUsername() : "");
    }

    private RestRequestDetails createRequestDetails(ContainerRequestContext requestContext, String body) {
        RestRequestDetails restRequest = new RestRequestDetails();
        restRequest.setRequestUri(requestContext.getUriInfo().getRequestUri().toString());
        restRequest.setBody(body);
        restRequest.setCookies(requestContext.getCookies().entrySet().stream().collect(Collectors.toMap(Entry::getKey, e -> e.getValue().toString())));
        restRequest.setHeaders(requestContext.getHeaders().entrySet().stream().filter(e -> !skippedHeadersList.contains(e.getKey())).collect(
                Collectors.toMap(Entry::getKey, e -> StringUtils.join(e.getValue(), ","))));
        MediaType mediaType = requestContext.getMediaType();
        restRequest.setMediaType(mediaType != null ? mediaType.toString() : "");
        restRequest.setMethod(requestContext.getMethod());
        return restRequest;
    }

    private RestResponseDetails createResponseDetails(ContainerResponseContext responseContext) {
        RestResponseDetails restResponse = new RestResponseDetails();
        restResponse.setStatusCode(responseContext.getStatus());
        restResponse.setStatusText(responseContext.getStatusInfo().toEnum().name());
        restResponse.setCookies(responseContext.getCookies().entrySet().stream().collect(Collectors.toMap(Entry::getKey, e -> e.getValue().toString())));
        restResponse.setHeaders(responseContext.getHeaders().entrySet().stream().filter(e -> !skippedHeadersList.contains(e.getKey())).collect(
                Collectors.toMap(Entry::getKey, e -> StringUtils.join(e.getValue(), ","))));
        MediaType mediaType = responseContext.getMediaType();
        restResponse.setMediaType(mediaType != null ? mediaType.toString() : "");
        return restResponse;
    }

    private InputStream logInboundEntity(StringBuilder content, InputStream stream, Charset charset) throws IOException {
        if (contentLogging) {
            if (!stream.markSupported()) {
                stream = new BufferedInputStream(stream);
            }
            stream.mark(MAX_CONTENT_LENGTH + 1);
            byte[] entity = new byte[MAX_CONTENT_LENGTH + 1];
            int entitySize = stream.read(entity);
            if (entitySize != -1) {
                content.append(new String(entity, 0, Math.min(entitySize, MAX_CONTENT_LENGTH), charset));
                if (entitySize > MAX_CONTENT_LENGTH) {
                    content.append("...more...");
                }
            }
            content.append('\n');
            stream.reset();
        }
        return stream;
    }

    private class LoggingStream extends FilterOutputStream {
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        LoggingStream(OutputStream inner) {
            super(inner);
        }

        StringBuffer getStringBuilder(Charset charset) {
            StringBuffer b = new StringBuffer();
            if (contentLogging) {
                byte[] entity = baos.toByteArray();
                b.append(new String(entity, 0, Math.min(entity.length, MAX_CONTENT_LENGTH), charset));
                if (entity.length > MAX_CONTENT_LENGTH) {
                    b.append("...more...");
                }
                b.append('\n');
            }
            return b;
        }

        @Override
        public void write(int i) throws IOException {
            if (contentLogging && baos.size() <= MAX_CONTENT_LENGTH) {
                baos.write(i);
            }
            out.write(i);
        }
    }
}
