package com.sequenceiq.it.cloudbreak.action.v4.audit;

import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Response;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.audit.AuditTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class AuditListAction implements Action<AuditTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditListAction.class);

    @Override
    public AuditTestDto action(TestContext testContext, AuditTestDto testDto, CloudbreakClient client) throws Exception {
        Collection<AuditEventV4Response> responses = client.getDefaultClient(testContext)
                .auditV4Endpoint()
                .getAuditEvents(client.getWorkspaceId(), testDto.getResourceType(), testDto.getResourceId(), null)
                .getResponses();
        testDto.setResponses(responses.stream().collect(Collectors.toSet()));
        Log.whenJson(LOGGER, " Audit listed successfully:\n", testDto.getResponses());
        return testDto;
    }
}
