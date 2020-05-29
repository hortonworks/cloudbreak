package com.sequenceiq.cloudbreak.audit.converter;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.audit.converter.auditeventname.rest.RestResourceAuditEventConverter;
import com.sequenceiq.cloudbreak.audit.model.ApiRequestData;
import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.audit.model.EventData;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestRequestDetails;

@Component
public class RestEventDataExtractor implements EventDataExtractor<StructuredRestCallEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestEventDataExtractor.class);

    @Value("${info.app.version:}")
    private String cbVersion;

    @Inject
    private Map<String, RestResourceAuditEventConverter> resourceAuditEventConverters;

    @Override
    public EventData eventData(StructuredRestCallEvent structuredEvent) {
        RestRequestDetails restRequest = structuredEvent.getRestCall().getRestRequest();
        boolean mutating = Set.of("POST", "PUT", "DELETE").contains(restRequest.getMethod());
        String userAgent = restRequest.getHeaders().get("user-agent");
        return ApiRequestData.builder()
                .withApiVersion(cbVersion)
                .withMutating(mutating)
                .withRequestParameters(restRequest.getRequestUri())
                .withUserAgent(userAgent)
                .build();
    }

    @Override
    public AuditEventName eventName(StructuredRestCallEvent structuredEvent) {
        String resourceType = structuredEvent.getOperation().getResourceType();
        RestResourceAuditEventConverter restResourceAuditEventConverter = getConverter(resourceType);
        if (restResourceAuditEventConverter != null) {
            LOGGER.info("Determine eventName with {}", restResourceAuditEventConverter);
            AuditEventName eventName = restResourceAuditEventConverter.auditEventName(structuredEvent);
            if (eventName != null) {
                return eventName;
            }
        }
        String method = structuredEvent.getRestCall().getRestRequest().getMethod();
        throw new UnsupportedOperationException(String.format("The `%s` with `%s` does not support for auditing", resourceType, method));
    }

    @Override
    public Crn.Service eventSource(StructuredRestCallEvent structuredEvent) {
        String resourceType = structuredEvent.getOperation().getResourceType();
        if (resourceType != null) {
            RestResourceAuditEventConverter restResourceAuditEventConverter = getConverter(resourceType);
            if (restResourceAuditEventConverter != null) {
                return restResourceAuditEventConverter.eventSource(structuredEvent);
            }
        }
        return null;
    }

    @Override
    public String sourceIp(StructuredRestCallEvent structuredEvent) {
        return structuredEvent.getRestCall().getRestRequest().getHeaders().get("x-real-ip");
    }

    @Override
    public boolean shouldAudit(StructuredEvent structuredEvent) {
        StructuredRestCallEvent event = (StructuredRestCallEvent) structuredEvent;
        String resourceType = event.getOperation().getResourceType();
        if (resourceType != null) {
            RestResourceAuditEventConverter restResourceAuditEventConverter = getConverter(resourceType);
            return restResourceAuditEventConverter != null && restResourceAuditEventConverter.shouldAudit(event);
        }
        return false;
    }

    private RestResourceAuditEventConverter getConverter(String resourceType) {
        return resourceAuditEventConverters.get(resourceType + "RestResourceAuditEventConverter");
    }
}
