package com.sequenceiq.it.cloudbreak.newway.action.v4.audit;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.audit.AuditTestDto;

public class AuditGetAction implements Action<AuditTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditGetAction.class);

    @Override
    public AuditTestDto action(TestContext testContext, AuditTestDto entity, CloudbreakClient client) throws Exception {
        AuditEventV4Response response = client.getCloudbreakClient()
                .auditV4Endpoint()
                .getAuditEventById(client.getWorkspaceId(), entity.getAuditId());
        entity.setResponse(response);
        logJSON(LOGGER, " Audit listed successfully:\n", entity.getResponse());
        return entity;
    }
}
