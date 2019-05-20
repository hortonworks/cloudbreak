package com.sequenceiq.it.cloudbreak.v4;

import static com.sequenceiq.it.cloudbreak.util.RetryOnGatewayTimeout.retry;

import java.io.IOException;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.Credential;
import com.sequenceiq.it.cloudbreak.Entity;
import com.sequenceiq.it.cloudbreak.Region;
import com.sequenceiq.it.cloudbreak.log.Log;

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

        if (credential != null && regionEntity.getPlatformResourceRequest().getCredentialName() == null) {
            regionEntity.getPlatformResourceRequest().setCredentialName(credential.getName());
        }

        Log.log(" get region to " + regionEntity.getPlatformResourceRequest().getCredentialName() + " credential. ");

        regionEntity.setRegionV4Response(retry(() -> client.getCloudbreakClient()
                .connectorV4Endpoint()
                        .getRegionsByCredential(workspaceId, regionEntity.getPlatformResourceRequest().getCredentialName(),
                                regionEntity.getPlatformResourceRequest().getRegion(), regionEntity.getPlatformResourceRequest().getPlatformVariant(),
                                regionEntity.getPlatformResourceRequest().getAvailabilityZone()),
                retryQuantity));

        Log.logJSON(" get region response: ", regionEntity.getRegionV4Response());
    }
}
