package com.sequenceiq.it.cloudbreak.newway.v4;

import static com.sequenceiq.it.cloudbreak.RetryOnGatewayTimeout.retry;

import java.io.IOException;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.Entity;
import com.sequenceiq.it.cloudbreak.newway.Region;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

public class RegionV4Action {

    private RegionV4Action() {
    }

    public static void getRegionsByCredentialId(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        getRegionsByCredentialId(integrationTestContext, entity, 1);
    }

    public static void getRegionsByCredentialId(IntegrationTestContext integrationTestContext, Entity entity, int retryQuantity) throws IOException {
        Region regionEntity = (Region) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);

        Credential credential = Credential.getTestContextCredential().apply(integrationTestContext);

        if (credential != null && regionEntity.getPlatformResourceRequest().getCredentialId() == null) {
            regionEntity.getPlatformResourceRequest().setCredentialName(credential.getName());
        }

        Log.log(" get region to " + regionEntity.getPlatformResourceRequest().getCredentialName() + " credential. ");

        regionEntity.setRegionV4Response(retry(() -> client.getCloudbreakClient()
                .connectorV4Endpoint()
                .getRegionsByCredential(workspaceId, regionEntity.getPlatformResourceRequest()), retryQuantity));

        Log.logJSON(" get region response: ", regionEntity.getRegionV4Response());
    }
}
