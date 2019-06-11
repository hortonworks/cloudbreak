package com.sequenceiq.it.cloudbreak.action.v4.audit;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.audit.AuditTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class AuditListZipAction implements Action<AuditTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditListZipAction.class);

    @Override
    public AuditTestDto action(TestContext testContext, AuditTestDto testDto, CloudbreakClient client) throws Exception {
        Response auditEventsZip = client.getCloudbreakClient()
                .auditV4Endpoint()
                .getAuditEventsZip(client.getWorkspaceId(), testDto.getResourceType(), testDto.getResourceId());
        testDto.setZipResponse(auditEventsZip);
        Log.logJSON(LOGGER, " Audit listed successfully:\n", testDto.getZipResponse());
        return testDto;
    }
}
