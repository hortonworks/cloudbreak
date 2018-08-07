package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import com.sequenceiq.it.IntegrationTestContext;

public class SshKeyAction {
    private SshKeyAction() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        SshKey sshKey = (SshKey) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);

        sshKey.setResponse(
                client.getCloudbreakClient()
                        .connectorV1Endpoint()
                        .getCloudSshKeys(sshKey.getRequest()));
        logJSON("V1 Connectors networks post request: ", sshKey.getRequest());
    }
}
