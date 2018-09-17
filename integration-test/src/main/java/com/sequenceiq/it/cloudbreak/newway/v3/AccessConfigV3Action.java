package com.sequenceiq.it.cloudbreak.newway.v3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sequenceiq.cloudbreak.api.model.PlatformResourceRequestJson;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.AccessConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.Entity;
import com.sequenceiq.it.cloudbreak.newway.Region;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

public class AccessConfigV3Action {

    private AccessConfigV3Action() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        AccessConfigEntity accessConfig = (AccessConfigEntity) entity;
        CloudbreakClient client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);

        PlatformResourceRequestJson request = new PlatformResourceRequestJson();

        Credential credential = Credential.getTestContextCredential().apply(integrationTestContext);
        if (credential != null && accessConfig.getRequest().getCredentialId() == null) {
            accessConfig.getRequest().setCredentialId(credential.getResponse().getId());
            request.setCredentialId(credential.getResponse().getId());
        }

        Region region = Region.getTestContextRegion().apply(integrationTestContext);
        if (region != null && accessConfig.getRequest().getRegion() == null) {
            accessConfig.getRequest().setRegion(region.getRegionResponse().getDefaultRegion());
            request.setRegion(region.getRegionResponse().getDefaultRegion());
        }

        accessConfig.setRequest(request);

        if (region != null && accessConfig.getRequest().getAvailabilityZone() == null) {
            Map<String, Collection<String>> regionsAvailabilityZones = region.getRegionResponse().getAvailabilityZones();
            List<String> availabilityZones = new ArrayList<>();

            for (Entry<String, Collection<String>> regionAvailabilityZones : regionsAvailabilityZones.entrySet()) {
                availabilityZones.addAll(regionAvailabilityZones.getValue());
            }
            accessConfig.getRequest().setAvailabilityZone(availabilityZones.get(1));
        }

        Log.log(String.join(" ", " post AccessConfig"));
        accessConfig.setResponse(
                client.getCloudbreakClient()
                        .connectorV3Endpoint().getAccessConfigs(workspaceId, accessConfig.getRequest()));
        Log.logJSON(" post AccessConfig response: ", accessConfig.getResponse());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        post(integrationTestContext, entity);
    }
}
