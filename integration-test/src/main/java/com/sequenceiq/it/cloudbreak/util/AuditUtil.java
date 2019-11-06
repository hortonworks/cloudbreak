package com.sequenceiq.it.cloudbreak.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.AuditEventV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceV4Responses;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.log.Log;

public class AuditUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditUtil.class);

    private AuditUtil() {

    }

    private static String getAuditEvents(AuditEventV4Endpoint endpoint, Long workspaceId, String resourceType, Long id, String crn) {
        AuditEventV4Responses events = endpoint.getAuditEvents(workspaceId, resourceType, id, crn);
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

        WorkspaceV4Responses workspaces = cloudbreakClient.getCloudbreakClient().workspaceV4Endpoint().list();
        // workaround: currently only one workspace per user, if there is more, we show this information in log
        Long workspaceId;
        if (workspaces.getResponses().size() == 1) {
            workspaceId = workspaces.getResponses().stream().findFirst().get().getId();
        } else {
            Log.log(LOGGER, "There is more than one workspace, cannot decide which is in use");
            workspaceId = cloudbreakClient.getWorkspaceId();
        }
        Log.log(LOGGER, "Workspace id for audit log: " + workspaceId);

        return getAuditEvents(endpoint, workspaceId, resourceType, id, crn);
    }
}
