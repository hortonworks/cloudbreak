package com.sequenceiq.it.cloudbreak.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.AuditEventV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Responses;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;

public class AuditUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditUtil.class);

    private AuditUtil() {

    }

    private static String getAuditEvents(AuditEventV4Endpoint endpoint, String resourceType, Long id, String crn) {
        AuditEventV4Responses events = endpoint.getAuditEvents(0L, resourceType, id, crn);
        String json = null;
        try {
            json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(events);
        } catch (JsonProcessingException e) {
            return null;
        }

        return json;
    }

    public static String getAuditEvents(CloudbreakClient cloudbreakClient, String resourceType, Long id, String crn) {
        AuditEventV4Endpoint endpoint = cloudbreakClient.getCloudbreakClient().auditV4Endpoint();
        return getAuditEvents(endpoint, resourceType, id, crn);
    }
}
