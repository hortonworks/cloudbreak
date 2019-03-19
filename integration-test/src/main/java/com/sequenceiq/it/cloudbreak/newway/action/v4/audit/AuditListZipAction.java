package com.sequenceiq.it.cloudbreak.newway.action.v4.audit;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.audit.AuditTestDto;

public class AuditListZipAction implements Action<AuditTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditListZipAction.class);

    @Override
    public AuditTestDto action(TestContext testContext, AuditTestDto testDto, CloudbreakClient client) throws Exception {
        Response auditEventsZip = client.getCloudbreakClient()
                .auditV4Endpoint()
                .getAuditEventsZip(client.getWorkspaceId(), testDto.getResourceType(), testDto.getResourceId());
        testDto.setZipResponse(auditEventsZip);
        logJSON(LOGGER, " Audit listed successfully:\n", testDto.getZipResponse());
        return testDto;
    }
}
