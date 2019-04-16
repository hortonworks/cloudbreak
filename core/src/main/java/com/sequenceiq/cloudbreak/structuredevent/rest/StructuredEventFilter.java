package com.sequenceiq.cloudbreak.structuredevent.rest;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.REST;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers.RestUrlParser.RESOURCE_ID;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers.RestUrlParser.RESOURCE_NAME;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers.RestUrlParser.RESOURCE_TYPE;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers.RestUrlParser.WORKSPACE_ID;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Path;
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
import org.apache.commons.lang3.math.NumberUtils;
import org.glassfish.jersey.message.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.ha.CloudbreakNodeConfig;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.structuredevent.StructuredEventClient;
import com.sequenceiq.cloudbreak.structuredevent.event.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestCallDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestRequestDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestResponseDetails;
import com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers.RestUrlParser;
import com.sequenceiq.cloudbreak.util.JsonUtil;
import com.sequenceiq.cloudbreak.util.WorkspaceAwareRepositoryLookupService;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

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

    private final List<String> skippedHeadersList = Lists.newArrayList("authorization");

    private final Map<String, WorkspaceResourceRepository<?, ?>> pathRepositoryMap = new HashMap<>();

    private final Pattern extendRestParamsFromResponsePattern = Pattern.compile("\"id\":([0-9]*)");

    @Inject
    private CloudbreakNodeConfig cloudbreakNodeConfig;

    @Value("${info.app.version:}")
    private String cbVersion;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    @Named("structuredEventClient")
    private StructuredEventClient structuredEventClient;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Autowired
    private List<RestUrlParser> restUrlParsers;

    @Value("${cb.structuredevent.rest.contentlogging:false}")
    private Boolean contentLogging;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ListableBeanFactory listableBeanFactory;

    @Autowired
    private WorkspaceAwareRepositoryLookupService repositoryLookupService;

    @PostConstruct
    public void initializePathRepositoryMap() {
        Map<String, Object> workspaceEntityTypes = listableBeanFactory.getBeansWithAnnotation(WorkspaceEntityType.class);
        for (Object workspaceEntityType : workspaceEntityTypes.values()) {
            Path pathAnnotation = AnnotationUtils.findAnnotation(workspaceEntityType.getClass().getSuperclass(), Path.class);
            WorkspaceEntityType entityTypeAnnotation = AnnotationUtils.findAnnotation(workspaceEntityType.getClass(), WorkspaceEntityType.class);
            if (pathAnnotation != null) {
                String pathValue = pathAnnotation.value();
                Class<?> entityClass = entityTypeAnnotation.value();
                WorkspaceResourceRepository<?, ?> repository = repositoryLookupService.getRepositoryForEntity(entityClass);
                pathRepositoryMap.put(pathValue, repository);
            }
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        boolean loggingEnabled = isLoggingEnabled(requestContext);
        requestContext.setProperty(LOGGING_ENABLED_PROPERTY, loggingEnabled);
        if (loggingEnabled) {
            requestContext.setProperty(REQUEST_TIME, System.currentTimeMillis());
            StringBuilder body = new StringBuilder();
            requestContext.setEntityStream(logInboundEntity(body, requestContext.getEntityStream(), MessageUtils.getCharset(requestContext.getMediaType())));
            requestContext.setProperty(REST_PARAMS, getRequestUrlParameters(requestContext));
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
            if (restParams == null) {
                restParams = new HashMap<>();
            }
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
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        if (cloudbreakUser == null) {
            String serviceId = authenticatedUserService.getServiceAccountId();
            cloudbreakUser = new CloudbreakUser(serviceId, serviceId, serviceId, serviceId, serviceId);
        }
        Long workspaceId = restRequestThreadLocalService.getRequestedWorkspaceId();
        structuredEventClient.sendStructuredEvent(new StructuredRestCallEvent(createOperationDetails(restParams, requestTime, workspaceId, cloudbreakUser),
                restCall));
    }

    private Map<String, String> getRequestUrlParameters(ContainerRequestContext requestContext) {
        Map<String, String> params = Maps.newHashMap();
        for (RestUrlParser restUrlParser : restUrlParsers) {
            if (restUrlParser.fillParams(requestContext, params)) {
                String workspaceId = params.get(WORKSPACE_ID);
                if (isResourceIdIsAbsentOrNull(params) && NumberUtils.isCreatable(workspaceId)) {
                    putResourceIdFromRepository(requestContext, params, restUrlParser, workspaceId);
                }
                break;
            }
        }
        return params;
    }

    private void putResourceIdFromRepository(ContainerRequestContext requestContext, Map<String, String> params,
            RestUrlParser restUrlParser, String workspaceId) {
        for (Entry<String, WorkspaceResourceRepository<?, ?>> pathRepositoryEntry : pathRepositoryMap.entrySet()) {
            String pathWithWorkspaceId = pathRepositoryEntry.getKey().replaceFirst("\\{.*\\}", workspaceId);
            String requestUrl = restUrlParser.getUrl(requestContext);
            if (('/' + requestUrl).startsWith(pathWithWorkspaceId)) {
                WorkspaceResourceRepository<?, ?> resourceRepository = pathRepositoryEntry.getValue();
                String resourceName = params.get(RESOURCE_NAME);
                if (resourceName != null) {
                    resourceRepository.findByNameAndWorkspaceId(resourceName, Long.valueOf(workspaceId))
                            .ifPresent(resource -> params.put(RESOURCE_ID, Long.toString(resource.getId())));
                }
                break;
            }
        }
    }

    protected void extendRestParamsFromResponse(Map<String, String> params, CharSequence responseBody) {
        if (responseBody != null && isResourceIdIsAbsentOrNull(params)) {
            String resourceId = extractResourceIdFromJson(responseBody);
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

    private String extractResourceIdFromJson(CharSequence responseBody) {
        String resourceId = null;
        try {
            if (JsonUtil.isValid(responseBody.toString())) {
                JsonNode jsonNode = JsonUtil.readTree(responseBody.toString());
                JsonNode idNode = jsonNode.path("id");
                if (idNode.isMissingNode()) {
                    LOGGER.debug("Response was a JSON but no ID available");
                } else {
                    resourceId = idNode.asText();
                }
            }
        } catch (IOException e) {
            LOGGER.error("Json parsing failed for ", e);
        }
        return resourceId;
    }

    private boolean isResourceIdIsAbsentOrNull(Map<String, String> params) {
        return !params.containsKey(RESOURCE_ID) || params.get(RESOURCE_ID) == null;
    }

    private boolean isLoggingEnabled(ContainerRequestContext requestContext) {
        return !"GET".equals(requestContext.getMethod());
    }

    private OperationDetails createOperationDetails(Map<String, String> restParams, Long requestTime, Long workspaceId, CloudbreakUser cloudbreakUser) {
        String resourceType = null;
        String resourceId = null;
        String resourceName = null;
        if (restParams != null) {
            resourceType = restParams.get(RESOURCE_TYPE);
            resourceId = restParams.get(RESOURCE_ID);
            resourceName = restParams.get(RESOURCE_NAME);
        }
        return new OperationDetails(requestTime, REST, resourceType, StringUtils.isNotEmpty(resourceId) ? Long.valueOf(resourceId) : null, resourceName,
                cloudbreakNodeConfig.getId(), cbVersion, workspaceId,
                cloudbreakUser != null ? cloudbreakUser.getUserId() : "", cloudbreakUser != null ? cloudbreakUser.getUsername() : "",
                cloudbreakUser.getTenant());
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
