package com.sequenceiq.cloudbreak.structuredevent.rest;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.REST;
import static com.sequenceiq.cloudbreak.structuredevent.filter.CDPJaxRsFilterPropertyKeys.REQUEST_DETAILS;
import static com.sequenceiq.cloudbreak.structuredevent.filter.CDPJaxRsFilterPropertyKeys.REQUEST_TIME;
import static com.sequenceiq.cloudbreak.structuredevent.filter.CDPJaxRsFilterPropertyKeys.RESPONSE_DETAILS;
import static com.sequenceiq.cloudbreak.structuredevent.filter.CDPJaxRsFilterPropertyKeys.RESPONSE_LOGGING_STREAM;
import static com.sequenceiq.cloudbreak.structuredevent.util.RestFilterRequestBodyLogger.logInboundEntity;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;

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
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.structuredevent.LegacyDefaultStructuredEventClient;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.legacy.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestCallDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestRequestDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestResponseDetails;
import com.sequenceiq.cloudbreak.structuredevent.filter.CDPJaxRsFilterOrder;
import com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.LegacyRestUrlParser;
import com.sequenceiq.cloudbreak.structuredevent.service.lookup.WorkspaceAwareRepositoryLookupService;
import com.sequenceiq.cloudbreak.structuredevent.util.LoggingStream;
import com.sequenceiq.cloudbreak.structuredevent.util.RestFilterPropertyUtil;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@Component
@Priority(CDPJaxRsFilterOrder.CDP_STRUCTURED_EVENT_FILTER_ORDER)
public class LegacyStructuredEventFilter implements WriterInterceptor, ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyStructuredEventFilter.class);

    private static final String LOGGING_ENABLED_PROPERTY = "structuredevent.loggingEnabled";

    private static final String REST_PARAMS = "REST_PARAMS";

    private static final String ID = "id";

    private static final String CRN = "crn";

    private final Map<String, WorkspaceResourceRepository<?, ?>> pathRepositoryMap = new HashMap<>();

    private final Pattern extractIdRestParamFromResponsePattern = Pattern.compile("\"" + ID + "\":([0-9]*)");

    private final Pattern extractCrnRestParamFromResponsePattern = Pattern.compile("\"" + CRN + "\":\"([0-9a-zA-Z:-]*)\"");

    @Value("${info.app.version:}")
    private String cbVersion;

    @Value("${cb.structuredevent.rest.contentlogging}")
    private Boolean contentLogging;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private NodeConfig nodeConfig;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private CloudbreakRestRequestThreadLocalService cloudbreakRestRequestThreadLocalService;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private LegacyDefaultStructuredEventClient legacyStructuredEventClient;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private List<LegacyRestUrlParser> legacyRestUrlParsers;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private ApplicationContext applicationContext;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private ListableBeanFactory listableBeanFactory;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
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
            requestContext.setEntityStream(
                    logInboundEntity(body, requestContext.getEntityStream(), MessageUtils.getCharset(requestContext.getMediaType()), contentLogging));
            requestContext.setProperty(REST_PARAMS, getRequestUrlParameters(requestContext));
            requestContext.setProperty(REQUEST_DETAILS, RestFilterPropertyUtil.createRequestDetails(requestContext, body.toString()));
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        if (BooleanUtils.isTrue((Boolean) requestContext.getProperty(LOGGING_ENABLED_PROPERTY))) {
            RestResponseDetails restResponse = RestFilterPropertyUtil.createResponseDetails(responseContext);
            if (responseContext.hasEntity()) {
                RestFilterPropertyUtil.extractAndSetResponseEntityStreamIfAbsent(requestContext, responseContext, contentLogging);
                RestFilterPropertyUtil.setPropertyIfAbsent(requestContext, RESPONSE_DETAILS, restResponse);
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
            LoggingStream responseLoggingStream = (LoggingStream) context.getProperty(RESPONSE_LOGGING_STREAM);
            String responseBody = null;
            if (responseLoggingStream != null) {
                responseBody = responseLoggingStream.getStringBuilder(MessageUtils.getCharset(context.getMediaType())).toString();
            }
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
        CloudbreakUser cloudbreakUser = cloudbreakRestRequestThreadLocalService.getCloudbreakUser();
        Long workspaceId = cloudbreakRestRequestThreadLocalService.getRequestedWorkspaceId();
        legacyStructuredEventClient.sendStructuredEvent(new StructuredRestCallEvent(createOperationDetails(restParams, requestTime, workspaceId, cloudbreakUser),
                restCall));
    }

    private Map<String, String> getRequestUrlParameters(ContainerRequestContext requestContext) {
        Map<String, String> params = Maps.newHashMap();
        for (LegacyRestUrlParser legacyRestUrlParser : legacyRestUrlParsers) {
            if (legacyRestUrlParser.fillParams(requestContext, params)) {
                String workspaceId = params.get(LegacyRestUrlParser.WORKSPACE_ID);
                if (isResourceIdIsAbsentOrNull(params) && NumberUtils.isCreatable(workspaceId)) {
                    putResourceIdFromRepository(requestContext, params, legacyRestUrlParser, workspaceId);
                }
                break;
            }
        }
        return params;
    }

    private void putResourceIdFromRepository(ContainerRequestContext requestContext, Map<String, String> params,
            LegacyRestUrlParser legacyRestUrlParser, String workspaceId) {
        for (Entry<String, WorkspaceResourceRepository<?, ?>> pathRepositoryEntry : pathRepositoryMap.entrySet()) {
            String pathWithWorkspaceId = pathRepositoryEntry.getKey().replaceFirst("\\{.*\\}", workspaceId);
            String requestUrl = legacyRestUrlParser.getUrl(requestContext);
            if (('/' + requestUrl).startsWith(pathWithWorkspaceId)) {
                WorkspaceResourceRepository<?, ?> resourceRepository = pathRepositoryEntry.getValue();
                String resourceName = params.get(LegacyRestUrlParser.RESOURCE_NAME);
                if (resourceName != null) {
                    resourceRepository.findByNameAndWorkspaceId(resourceName, Long.valueOf(workspaceId))
                            .ifPresent(resource -> params.put(LegacyRestUrlParser.RESOURCE_ID, Long.toString(resource.getId())));
                }
                break;
            }
        }
    }

    protected void extendRestParamsFromResponse(Map<String, String> params, CharSequence responseBody) {
        boolean resourceIdIsAbsentOrNull = isResourceIdIsAbsentOrNull(params);
        boolean resourceCrnIsAbsentOrNull = isResourceCrnIsAbsentOrNull(params);
        if (responseBody != null && (resourceIdIsAbsentOrNull || resourceCrnIsAbsentOrNull)) {
            Set<String> resourcesParamsToCollect = new HashSet<>();
            decorateSetWithValueIfNecessary(resourceIdIsAbsentOrNull, ID, resourcesParamsToCollect);
            decorateSetWithValueIfNecessary(resourceCrnIsAbsentOrNull, CRN, resourcesParamsToCollect);
            Map<String, String> resourceParams = extractResourceValueFromJson(responseBody, resourcesParamsToCollect);
            extractResourceParamWithPattern(responseBody, resourceIdIsAbsentOrNull, resourceParams, ID, extractIdRestParamFromResponsePattern);
            extractResourceParamWithPattern(responseBody, resourceCrnIsAbsentOrNull, resourceParams, CRN, extractCrnRestParamFromResponsePattern);
            addExtractedValuesToParameters(params, resourceParams, ID, LegacyRestUrlParser.RESOURCE_ID);
            addExtractedValuesToParameters(params, resourceParams, CRN, LegacyRestUrlParser.RESOURCE_CRN);
        }
    }

    private void addExtractedValuesToParameters(Map<String, String> params, Map<String, String> resourceParams, String id, String resourceId) {
        if (resourceParams.containsKey(id)) {
            params.put(resourceId, resourceParams.get(id));
        }
    }

    private void extractResourceParamWithPattern(CharSequence responseBody, boolean resourceIdIsAbsentOrNull, Map<String, String> resourceParams, String key,
            Pattern pattern) {
        if (resourceIdIsAbsentOrNull && !resourceParams.containsKey(key)) {
            Matcher matcher = pattern.matcher(responseBody);
            if (matcher.find() && matcher.groupCount() >= 1) {
                resourceParams.put(key, matcher.group(1));
            }
        }
    }

    private void decorateSetWithValueIfNecessary(boolean decorate, String value, Set<String> set) {
        if (decorate) {
            set.add(value);
        }
    }

    private Map<String, String> extractResourceValueFromJson(CharSequence responseBody, Set<String> pathes) {
        Map<String, String> resourceMap = new HashMap<>(pathes.size());
        try {
            if (JsonUtil.isValid(responseBody.toString())) {
                JsonNode jsonNode = JsonUtil.readTree(responseBody.toString());
                for (String path : pathes) {
                    JsonNode idNode = jsonNode.path(path);
                    if (idNode.isMissingNode()) {
                        LOGGER.debug("Response was a JSON but no " + path + " available");
                    } else {
                        resourceMap.put(path, idNode.asText());
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Json parsing failed for ", e);
        }
        return resourceMap;
    }

    private boolean isResourceIdIsAbsentOrNull(Map<String, String> params) {
        return isParameterAbsentOrNull(params, LegacyRestUrlParser.RESOURCE_ID);
    }

    private boolean isParameterAbsentOrNull(Map<String, String> params, String parameter) {
        return !params.containsKey(parameter) || params.get(parameter) == null;
    }

    private boolean isResourceCrnIsAbsentOrNull(Map<String, String> params) {
        return isParameterAbsentOrNull(params, LegacyRestUrlParser.RESOURCE_CRN);
    }

    private boolean isLoggingEnabled(ContainerRequestContext requestContext) {
        return !"GET".equals(requestContext.getMethod());
    }

    private OperationDetails createOperationDetails(Map<String, String> restParams, Long requestTime, Long workspaceId, CloudbreakUser cloudbreakUser) {
        String resourceType = null;
        String resourceId = null;
        String resourceName = null;
        String resourceCrn = null;
        String resourceEvent = null;
        if (restParams != null) {
            resourceType = restParams.get(LegacyRestUrlParser.RESOURCE_TYPE);
            resourceId = restParams.get(LegacyRestUrlParser.RESOURCE_ID);
            resourceName = restParams.get(LegacyRestUrlParser.RESOURCE_NAME);
            resourceCrn = restParams.get(LegacyRestUrlParser.RESOURCE_CRN);
            resourceEvent = restParams.get(LegacyRestUrlParser.RESOURCE_EVENT);
        }
        return new OperationDetails(requestTime,
                REST,
                resourceType,
                StringUtils.isNotEmpty(resourceId) ? Long.valueOf(resourceId) : null,
                resourceName,
                nodeConfig.getId(),
                cbVersion,
                workspaceId,
                cloudbreakUser != null ? cloudbreakUser.getUserId() : "",
                cloudbreakUser != null ? cloudbreakUser.getUsername() : "",
                cloudbreakUser != null ? cloudbreakUser.getTenant() : "",
                resourceCrn,
                cloudbreakUser != null ? cloudbreakUser.getUserCrn() : "",
                null,
                resourceEvent);

    }
}
