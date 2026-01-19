package com.sequenceiq.it.cloudbreak.action.v4.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Response;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.audit.AuditTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class AuditGetAction implements Action<AuditTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditGetAction.class);

    @Override
    public AuditTestDto action(TestContext testContext, AuditTestDto testDto, CloudbreakClient client) throws Exception {
        AuditEventV4Response response = client.getDefaultClient(testContext)
                .auditV4Endpoint()
                .getAuditEventById(client.getWorkspaceId(), testDto.getAuditId());
        testDto.setResponse(response);
        Log.whenJson(LOGGER, " Audit listed successfully:\n", testDto.getResponse());
        return testDto;
    }
}
