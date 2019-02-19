package com.sequenceiq.it.cloudbreak.newway.action.audit;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import java.util.Collection;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.audit.AuditTestDto;

public class AuditTestAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditTestAction.class);

    private AuditTestAction() {

    }

    public static AuditTestDto getAuditEvents(TestContext testContext, AuditTestDto entity, CloudbreakClient client) throws Exception {
        Collection<AuditEventV4Response> responses = client.getCloudbreakClient()
                .auditV4Endpoint()
                .getAuditEvents(client.getWorkspaceId(), entity.getResourceType(), entity.getResourceId())
                .getResponses();
        entity.setResponses(responses.stream().collect(Collectors.toSet()));
        logJSON(LOGGER, " Audit listed successfully:\n", entity.getResponses());
        return entity;
    }

    public static AuditTestDto getAuditEventById(TestContext testContext, AuditTestDto entity, CloudbreakClient client) throws Exception {
        AuditEventV4Response response = client.getCloudbreakClient()
                .auditV4Endpoint()
                .getAuditEventById(client.getWorkspaceId(), entity.getAuditId());
        entity.setResponse(response);
        logJSON(LOGGER, " Audit listed successfully:\n", entity.getResponse());
        return entity;
    }

    public static AuditTestDto getAuditEventsZip(TestContext testContext, AuditTestDto entity, CloudbreakClient client) throws Exception {
        Response auditEventsZip = client.getCloudbreakClient()
                .auditV4Endpoint()
                .getAuditEventsZip(client.getWorkspaceId(), entity.getResourceType(), entity.getResourceId());
        entity.setZipResponse(auditEventsZip);
        logJSON(LOGGER, " Audit listed successfully:\n", entity.getZipResponse());
        return entity;
    }
}
