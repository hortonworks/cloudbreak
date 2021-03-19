package com.sequenceiq.it.cloudbreak.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.AuditEventV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Responses;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;

public class AuditUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditUtil.class);

    private AuditUtil() {

    }

    public static AuditEventV4Responses getAuditEvents(CloudbreakClient cloudbreakClient, String resourceType, Long id, String crn) {
        AuditEventV4Endpoint endpoint = cloudbreakClient.getDefaultClient().auditV4Endpoint();
        return endpoint.getAuditEvents(0L, resourceType, id, crn);
    }
}
