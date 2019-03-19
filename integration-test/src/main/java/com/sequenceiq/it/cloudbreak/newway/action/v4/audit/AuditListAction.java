package com.sequenceiq.it.cloudbreak.newway.action.v4.audit;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.audit.AuditTestDto;

public class AuditListAction implements Action<AuditTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditListAction.class);

    @Override
    public AuditTestDto action(TestContext testContext, AuditTestDto testDto, CloudbreakClient client) throws Exception {
        Collection<AuditEventV4Response> responses = client.getCloudbreakClient()
                .auditV4Endpoint()
                .getAuditEvents(client.getWorkspaceId(), testDto.getResourceType(), testDto.getResourceId())
                .getResponses();
        testDto.setResponses(responses.stream().collect(Collectors.toSet()));
        logJSON(LOGGER, " Audit listed successfully:\n", testDto.getResponses());
        return testDto;
    }
}
