package com.sequenceiq.cloudbreak.structuredevent.rest.filter;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.REST;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser.RESOURCE_CRN;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser.RESOURCE_EVENT;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser.RESOURCE_ID;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser.RESOURCE_NAME;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser.RESOURCE_TYPE;

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
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.message.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestCallDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestRequestDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestResponseDetails;
import com.sequenceiq.cloudbreak.structuredevent.repository.AccountAwareResourceRepository;
import com.sequenceiq.cloudbreak.structuredevent.rest.CDPRestCommonService;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser;
import com.sequenceiq.cloudbreak.structuredevent.service.CDPBaseRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.structuredevent.service.CDPDefaultStructuredEventClient;
import com.sequenceiq.cloudbreak.structuredevent.service.lookup.CDPAccountAwareRepositoryLookupService;
import com.sequenceiq.flow.ha.NodeConfig;

@Component
public class CDPStructuredEventFilter implements WriterInterceptor, ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPStructuredEventFilter.class);

    private static final String LOGGING_ENABLED_PROPERTY = "structuredevent.loggingEnabled";

    private static final String LOGGINGSTREAM_PROPERTY = "structuredevent.entityLogger";

    private static final String REST_PARAMS = "REST_PARAMS";

    private static final String REQUEST_TIME = "REQUEST_TIME";

    private static final String REQUEST_DETAILS = "REQUEST_DETAIS";

    private static final String RESPONSE_DETAILS = "RESPONSE_DETAIS";

    private static final int MAX_CONTENT_LENGTH = 65535;

    private static final String ID = "id";

    private static final String CRN = "crn";

    private final List<String> skippedHeadersList = Lists.newArrayList("authorization");

    private final Map<String, AccountAwareResourceRepository<?, ?>> pathRepositoryMap = new HashMap<>();

    @Value("${info.app.version:}")
    private String cbVersion;

    @Value("${cdp.structuredevent.rest.contentlogging}")
    private Boolean contentLogging;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private NodeConfig nodeConfig;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private CDPBaseRestRequestThreadLocalService cloudbreakRestRequestThreadLocalService;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private CDPDefaultStructuredEventClient structuredEventClient;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private List<CDPRestUrlParser> cdpRestUrlParsers;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private ApplicationContext applicationContext;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private ListableBeanFactory listableBeanFactory;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private CDPAccountAwareRepositoryLookupService repositoryLookupService;

    @Autowired
    private CDPRestCommonService restCommonService;

    @PostConstruct
    public void initializePathRepositoryMap() {
        Map<String, Object> accountEntityTypes = listableBeanFactory.getBeansWithAnnotation(AccountEntityType.class);
        for (Object accountEntityType : accountEntityTypes.values()) {
            Path pathAnnotation = AnnotationUtils.findAnnotation(accountEntityType.getClass().getSuperclass(), Path.class);
            AccountEntityType entityTypeAnnotation = AnnotationUtils.findAnnotation(accountEntityType.getClass(), AccountEntityType.class);
            if (pathAnnotation != null) {
                String pathValue = pathAnnotation.value();
                Class<?> entityClass = entityTypeAnnotation.value();
                AccountAwareResourceRepository<?, ?> repository = repositoryLookupService.getRepositoryForEntity(entityClass);
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
            sendStructuredEvent(restRequest, restResponse, restParams, requestTime, responseBody);
        }
    }

    private void sendStructuredEvent(RestRequestDetails restRequest, RestResponseDetails restResponse, Map<String, String> restParams, Long requestTime,
            String responseBody) {
        boolean valid = checkRestParams(restParams);
        try {
            if (!valid) {
                LOGGER.debug("Cannot create structured event, because rest params are invalid.");
                return;
            }
            restResponse.setBody(responseBody);
            RestCallDetails restCall = new RestCallDetails();
            restCall.setRestRequest(restRequest);
            restCall.setRestResponse(restResponse);
            restCall.setDuration(System.currentTimeMillis() - requestTime);
            CloudbreakUser cloudbreakUser = cloudbreakRestRequestThreadLocalService.getCloudbreakUser();
            if (cloudbreakUser == null) {
                String serviceId = authenticatedUserService.getServiceAccountId();
                    cloudbreakUser = new CloudbreakUser(serviceId, serviceId, serviceId, serviceId, serviceId);
            }
            Map<String, String> params = restCommonService.collectCrnAndNameIfPresent(restCall, null, restParams, RESOURCE_NAME, RESOURCE_CRN);
            fetchDataFromDbIfNeed(params);
            restParams.putAll(params);
            CDPOperationDetails cdpOperationDetails = createCDPOperationDetails(restParams, requestTime, cloudbreakUser);
            CDPStructuredRestCallEvent structuredEvent = new CDPStructuredRestCallEvent(cdpOperationDetails, restCall);
            structuredEventClient.sendStructuredEvent(structuredEvent);
        } catch (Exception ex) {
            LOGGER.warn("Failed to send structured event: " + ex.getMessage(), ex);
        }
    }

    private boolean checkRestParams(Map<String, String> restParams) {
        boolean empty = restParams.isEmpty();
        if (empty) {
            LOGGER.debug("Rest param is empty");
            return false;
        }
        boolean allNull = restParams.values().stream().allMatch(Objects::isNull);
        if (allNull) {
            LOGGER.debug("Rest param is not empty but all values are null");
            return false;
        }
        return true;
    }

    private Map<String, String> getRequestUrlParameters(ContainerRequestContext requestContext) {
        Map<String, String> params = Maps.newHashMap();
        for (CDPRestUrlParser cdpRestUrlParser : cdpRestUrlParsers) {
            if (cdpRestUrlParser.fillParams(requestContext, params)) {
                break;
            }
        }
        return params;
    }

    private void fetchDataFromDbIfNeed(Map<String, String> params) {
        for (Entry<String, AccountAwareResourceRepository<?, ?>> pathRepositoryEntry : pathRepositoryMap.entrySet()) {
            AccountAwareResourceRepository<?, ?> resourceRepository = pathRepositoryEntry.getValue();
            String resourceCrn = params.get(RESOURCE_CRN);
            String resourceName = params.get(RESOURCE_NAME);
            if (resourceCrn == null && resourceName != null) {
                String accountId = ThreadBasedUserCrnProvider.getAccountId();
                resourceRepository.findByNameAndAccountId(resourceName, accountId)
                        .ifPresent(resource -> {
                            params.put(RESOURCE_ID, Long.toString(resource.getId()));
                            params.put(RESOURCE_CRN, resource.getResourceCrn());
                        });
            } else if (resourceName == null) {
                String accountId = ThreadBasedUserCrnProvider.getAccountId();
                resourceRepository.findByResourceCrnAndAccountId(resourceCrn, accountId)
                        .ifPresent(resource -> {
                            params.put(RESOURCE_ID, Long.toString(resource.getId()));
                            params.put(RESOURCE_NAME, resource.getName());
                        });
            }
            break;
        }
    }

    private boolean isLoggingEnabled(ContainerRequestContext requestContext) {
        return !"GET".equals(requestContext.getMethod());
    }

    private CDPOperationDetails createCDPOperationDetails(Map<String, String> restParams, Long requestTime, CloudbreakUser cloudbreakUser) {
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
                StringUtils.isNotEmpty(cloudbreakUser.getTenant()) ? cloudbreakUser.getTenant() : ThreadBasedUserCrnProvider.getAccountId(),
                resourceCrn,
                StringUtils.isNotEmpty(cloudbreakUser.getUserCrn()) ? cloudbreakUser.getUserCrn() : ThreadBasedUserCrnProvider.getUserCrn(),
//                cloudbreakUser.getUserCrn(),
                null,
                resourceEvent);

    }

    private RestRequestDetails createRequestDetails(ContainerRequestContext requestContext, String body) {
        LOGGER.debug("Request body length: {}", body.length());
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
            String entityString = IOUtils.toString(stream, charset);
            if (entityString.length() > MAX_CONTENT_LENGTH) {
                entityString = entityString.substring(0, MAX_CONTENT_LENGTH) + "...more...";
            }
            content.append(entityString);
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
