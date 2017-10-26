package com.sequenceiq.cloudbreak.structuredevent.rest;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.ha.CloudbreakNodeConfig;
import com.sequenceiq.cloudbreak.structuredevent.StructuredEventClient;
import com.sequenceiq.cloudbreak.structuredevent.event.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestCallDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestRequestDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestResponseDetails;

@Component
public class StructuredEventFilter implements WriterInterceptor, ContainerRequestFilter, ContainerResponseFilter {
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

    @Inject
    private CloudbreakNodeConfig cloudbreakNodeConfig;

    @Value("${info.app.version:}")
    private String cbVersion;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    @Named("structuredEventClient")
    private StructuredEventClient structuredEventClient;

    @Value("${cb.structuredevent.rest.contentlogging:false}")
    private Boolean contentLogging;

    private List<String> urlBlackList = Lists.newArrayList("connectors", "flexsubscriptions", "accountpreferences", "users");

    private List<String> skippedHeadersList = Lists.newArrayList("authorization");

    private List<String> postUrlPatterns = Lists.newArrayList("\\/cb\\/api\\/v.\\/([a-z]*).*");

    private List<String> deleteUrlPatterns = Lists.newArrayList(
            "\\/cb\\/api\\/v.\\/([a-z]*)\\/([0-9]+)",
            "\\/cb\\/api\\/v.\\/([a-z]*)\\/(user|account)\\/(.*)");

    private List<String> putUrlPatterns = Lists.newArrayList(
            "\\/cb\\/api\\/v.\\/([a-z]*)\\/([0-9]+).*");

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
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        if (BooleanUtils.isTrue((Boolean) requestContext.getProperty(LOGGING_ENABLED_PROPERTY))) {
            RestResponseDetails restResponse = createResponseDetails(responseContext);
            if (responseContext.hasEntity()) {
                final OutputStream stream = new LoggingStream(responseContext.getEntityStream());
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
        structuredEventClient.sendStructuredEvent(new StructuredRestCallEvent(createOperationDetails(restParams, requestTime), restCall));
    }

    private Map<String, String> getRequestUrlParameters(String method, String url) {
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

    private Map<String, String> extendRestParamsFromResponse(Map<String, String> params, String responseBody) {
        if (!params.containsKey(RESOURCE_ID)) {
            Pattern pattern = Pattern.compile("\"id\":([0-9]*)");
            Matcher matcher = pattern.matcher(responseBody);
            if (matcher.find() && matcher.groupCount() >= 1) {
                params.put(RESOURCE_ID, matcher.group(1));
            }
        }
        return params;
    }

    private boolean isLoggingEnabled(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getRequestUri().getPath();
        return !requestContext.getMethod().equals("GET") && !urlBlackList.stream().anyMatch(url -> path.contains(url));
    }

    private OperationDetails createOperationDetails(Map<String, String> restParams, Long requestTime) {
        IdentityUser user = authenticatedUserService.getCbUser();
        String resoureceId = restParams.get(RESOURCE_ID);
        return new OperationDetails(requestTime, "REST", restParams.get(RESOURCE_TYPE), resoureceId != null ? Long.valueOf(resoureceId) : null,
                user != null ? user.getAccount() : "", user != null ? user.getUserId() : "", cloudbreakNodeConfig.getId(), cbVersion);
    }

    private RestRequestDetails createRequestDetails(ContainerRequestContext requestContext, String body) {
        RestRequestDetails restRequest = new RestRequestDetails();
        restRequest.setRequestUri(requestContext.getUriInfo().getRequestUri().toString());
        restRequest.setBody(body);
        restRequest.setCookies(requestContext.getCookies().entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toString())));
        restRequest.setHeaders(requestContext.getHeaders().entrySet().stream().filter(e -> !skippedHeadersList.contains(e.getKey())).collect(
                Collectors.toMap(e -> e.getKey(), e -> StringUtils.join(e.getValue(), ","))));
        MediaType mediaType = requestContext.getMediaType();
        restRequest.setMediaType(mediaType != null ? mediaType.toString() : "");
        restRequest.setMethod(requestContext.getMethod());
        return restRequest;
    }

    private RestResponseDetails createResponseDetails(ContainerResponseContext responseContext) {
        RestResponseDetails restResponse = new RestResponseDetails();
        restResponse.setStatusCode(responseContext.getStatus());
        restResponse.setCookies(responseContext.getCookies().entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toString())));
        restResponse.setHeaders(responseContext.getHeaders().entrySet().stream().filter(e -> !skippedHeadersList.contains(e.getKey())).collect(
                Collectors.toMap(e -> e.getKey(), e -> StringUtils.join(e.getValue(), ","))));
        MediaType mediaType = responseContext.getMediaType();
        restResponse.setMediaType(mediaType != null ? mediaType.toString() : "");
        return restResponse;
    }

    private InputStream logInboundEntity(final StringBuilder content, InputStream stream, final Charset charset) throws IOException {
        if (contentLogging) {
            if (!stream.markSupported()) {
                stream = new BufferedInputStream(stream);
            }
            stream.mark(MAX_CONTENT_LENGTH + 1);
            final byte[] entity = new byte[MAX_CONTENT_LENGTH + 1];
            final int entitySize = stream.read(entity);
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
        private int maxEntitySize = MAX_CONTENT_LENGTH;

        private final StringBuilder b;

        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        LoggingStream(final OutputStream inner) {
            super(inner);
            this.b = new StringBuilder();
        }

        StringBuilder getStringBuilder(final Charset charset) {
            if (contentLogging) {
                final byte[] entity = baos.toByteArray();
                b.append(new String(entity, 0, Math.min(entity.length, maxEntitySize), charset));
                if (entity.length > maxEntitySize) {
                    b.append("...more...");
                }
                b.append('\n');
            }
            return b;
        }

        @Override
        public void write(final int i) throws IOException {
            if (contentLogging && baos.size() <= maxEntitySize) {
                baos.write(i);
            }
            out.write(i);
        }
    }
}
