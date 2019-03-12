package com.sequenceiq.it.cloudbreak.newway.v4;

import org.apache.commons.lang3.NotImplementedException;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.Entity;

public class SshKeyV4Action {
    private SshKeyV4Action() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) {
        throw new NotImplementedException("Should figure out how to obtain the following values: region, availabilityZone, credentialName, platformVariant");
        /*SshKey sshKey = (SshKey) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);

        sshKey.setResponse(
                client.getCloudbreakClient()
                        .connectorV4Endpoint()
                        .getCloudSshKeys(workspaceId, sshKey.getRequest()));
        logJSON("V3 Connectors networks post request: ", sshKey.getRequest());*/
    }
}
