package com.sequenceiq.cloudbreak.structuredevent.service.audit.extractor;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredRestCallEvent;

@Component
public class RequestMethodBasedRestAuditEventNameExtractor {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestMethodBasedRestAuditEventNameExtractor.class);

    public AuditEventName getEventNameBasedOnRequestMethod(CDPStructuredRestCallEvent data) {
        AuditEventName result = AuditEventName.REST_AUDIT_UNKNOWN;
        if (data != null && data.getRestCall() != null && data.getRestCall().getRestRequest() != null) {
            String method = data.getRestCall().getRestRequest().getMethod();
            if (StringUtils.isNotEmpty(method)) {
                switch (method) {
                    case "GET":
                        result = AuditEventName.REST_AUDIT_GET;
                        break;
                    case "POST":
                        result = AuditEventName.REST_AUDIT_POST;
                        break;
                    case "PUT":
                        result = AuditEventName.REST_AUDIT_PUT;
                        break;
                    case "DELETE":
                        result = AuditEventName.REST_AUDIT_DELETE;
                        break;
                    default:
                        result = AuditEventName.REST_AUDIT_UNKNOWN;
                        break;
                }
                LOGGER.trace("The current request method from request data: '{}', the determined event name: '{}'", method, result);
            }
        }
        return result;
    }
}
