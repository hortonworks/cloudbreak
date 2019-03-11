package com.sequenceiq.it.cloudbreak.newway.action.v4.audit;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.audit.AuditTestDto;

public class AuditListZipAction implements Action<AuditTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditListZipAction.class);

    @Override
    public AuditTestDto action(TestContext testContext, AuditTestDto entity, CloudbreakClient client) throws Exception {
        Response auditEventsZip = client.getCloudbreakClient()
                .auditV4Endpoint()
                .getAuditEventsZip(client.getWorkspaceId(), entity.getResourceType(), entity.getResourceId());
        entity.setZipResponse(auditEventsZip);
        logJSON(LOGGER, " Audit listed successfully:\n", entity.getZipResponse());
        return entity;
    }
}
