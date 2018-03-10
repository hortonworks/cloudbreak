package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

import java.io.IOException;

public class RegionAction {

    private RegionAction() {
    }

    public static void getRegionsByCredentialId(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        Region regionEntity = (Region) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);

        Credential credential = Credential.getTestContextCredential().apply(integrationTestContext);

        if (credential != null && regionEntity.getPlatformResourceRequest().getCredentialId() == null) {
            regionEntity.getPlatformResourceRequest().setCredentialName(credential.getName());
        }

        Log.log(" get region to " + regionEntity.getPlatformResourceRequest().getCredentialName() + " credential. ");
        regionEntity.setRegionResponse(client.getCloudbreakClient()
                .connectorV2Endpoint()
                .getRegionsByCredentialId(regionEntity.getPlatformResourceRequest())
        );
        Log.logJSON(" get region response: ", regionEntity.getRegionResponse());
    }
}
