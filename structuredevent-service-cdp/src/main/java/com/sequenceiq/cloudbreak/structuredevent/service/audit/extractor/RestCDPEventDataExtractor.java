package com.sequenceiq.cloudbreak.structuredevent.service.audit.extractor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.audit.model.ApiRequestData;
import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.audit.model.EventData;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestRequestDetails;
import com.sequenceiq.cloudbreak.structuredevent.service.audit.CDPEventDataExtractor;
import com.sequenceiq.cloudbreak.structuredevent.service.audit.auditeventname.rest.CDPRestResourceAuditEventConverter;

@Component
public class RestCDPEventDataExtractor implements CDPEventDataExtractor<CDPStructuredRestCallEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestCDPEventDataExtractor.class);

    @Value("${info.app.version:}")
    private String cbVersion;

    @Inject
    private Map<String, CDPRestResourceAuditEventConverter> resourceAuditEventConverters;

    @Override
    public EventData eventData(CDPStructuredRestCallEvent structuredEvent) {
        RestRequestDetails restRequest = structuredEvent.getRestCall().getRestRequest();
        boolean mutating = Set.of("POST", "PUT", "DELETE").contains(restRequest.getMethod());
        String userAgent = restRequest.getHeaders().get("user-agent");

        Map<String, Object> requestParameters = new HashMap<>();
        requestParameters.put("uri", restRequest.getRequestUri());
        String resourceType = structuredEvent.getOperation().getResourceType();
        CDPRestResourceAuditEventConverter restResourceAuditEventConverter = getConverter(resourceType);
        if (restResourceAuditEventConverter != null) {
            LOGGER.info("Determine request params with {}", restResourceAuditEventConverter);
            Map<String, String> params = restResourceAuditEventConverter.requestParameters(structuredEvent);
            requestParameters.putAll(params);
        }
        return ApiRequestData.builder()
                .withApiVersion(cbVersion)
                .withMutating(mutating)
                .withRequestParameters(new Json(requestParameters).getValue())
                .withUserAgent(userAgent)
                .build();
    }

    @Override
    public AuditEventName eventName(CDPStructuredRestCallEvent structuredEvent) {
        AuditEventName auditEventName = determineEventName(structuredEvent);
        if (auditEventName != null) {
            LOGGER.info("Determined event name: {}", auditEventName);
            return auditEventName;
        }
        String resourceType = structuredEvent.getOperation().getResourceType();
        String method = structuredEvent.getRestCall().getRestRequest().getMethod();
        throw new UnsupportedOperationException(String.format("The `%s` with `%s` does not support for auditing", resourceType, method));
    }

    @Override
    public Crn.Service eventSource(CDPStructuredRestCallEvent structuredEvent) {
        String resourceType = structuredEvent.getOperation().getResourceType();
        if (resourceType != null) {
            CDPRestResourceAuditEventConverter restResourceAuditEventConverter = getConverter(resourceType);
            if (restResourceAuditEventConverter != null) {
                Crn.Service service = restResourceAuditEventConverter.eventSource(structuredEvent);
                LOGGER.info("Determined event source service: {}", service.getName());
                return service;
            }
        }
        return null;
    }

    @Override
    public String sourceIp(CDPStructuredRestCallEvent structuredEvent) {
        return structuredEvent.getRestCall().getRestRequest().getHeaders().get("x-real-ip");
    }

    @Override
    public boolean shouldAudit(CDPStructuredEvent structuredEvent) {
        CDPStructuredRestCallEvent event = (CDPStructuredRestCallEvent) structuredEvent;
        String resourceType = event.getOperation().getResourceType();
        if (resourceType != null) {
            CDPRestResourceAuditEventConverter restResourceAuditEventConverter = getConverter(resourceType);
            return restResourceAuditEventConverter != null && restResourceAuditEventConverter.shouldAudit(event);
        }
        return false;
    }

    private AuditEventName determineEventName(CDPStructuredRestCallEvent structuredEvent) {
        String resourceType = structuredEvent.getOperation().getResourceType();
        CDPRestResourceAuditEventConverter restResourceAuditEventConverter = getConverter(resourceType);
        if (restResourceAuditEventConverter != null) {
            LOGGER.info("Determine eventName with {}", restResourceAuditEventConverter);
            return restResourceAuditEventConverter.auditEventName(structuredEvent);
        }
        return null;
    }

    private CDPRestResourceAuditEventConverter getConverter(String resourceType) {
        return resourceAuditEventConverters.get(resourceType + "RestResourceAuditEventConverter");
    }
}
