package com.sequenceiq.it.cloudbreak.newway.v4;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Entity;
import com.sequenceiq.it.cloudbreak.newway.SshKey;

public class SshKeyV4Action {
    private SshKeyV4Action() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        SshKey sshKey = (SshKey) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);

        sshKey.setResponse(
                client.getCloudbreakClient()
                        .connectorV4Endpoint()
                        .getCloudSshKeys(workspaceId, sshKey.getRequest()));
        logJSON("V3 Connectors networks post request: ", sshKey.getRequest());
    }
}
