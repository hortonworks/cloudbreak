package com.sequenceiq.it.cloudbreak.newway.v4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.Entity;
import com.sequenceiq.it.cloudbreak.newway.PlatformResourceParameters;
import com.sequenceiq.it.cloudbreak.newway.Region;
import com.sequenceiq.it.cloudbreak.newway.dto.AccessConfigTestDto;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

public class AccessConfigV4Action {

    private AccessConfigV4Action() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        AccessConfigTestDto accessConfig = (AccessConfigTestDto) entity;
        CloudbreakClient client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);

        PlatformResourceParameters request = new PlatformResourceParameters();

        Credential credential = Credential.getTestContextCredential().apply(integrationTestContext);
        if (credential != null && accessConfig.getRequest().getCredentialName() == null) {
            accessConfig.getRequest().setCredentialName(credential.getResponse().getName());
            request.setCredentialName(credential.getResponse().getName());
        }

        Region region = Region.getTestContextRegion().apply(integrationTestContext);
        if (region != null && accessConfig.getRequest().getRegion() == null) {
            accessConfig.getRequest().setRegion(region.getRegionV4Response().getDefaultRegion());
            request.setRegion(region.getRegionV4Response().getDefaultRegion());
        }

        accessConfig.setRequest(request);

        if (region != null && accessConfig.getRequest().getAvailabilityZone() == null) {
            Map<String, Collection<String>> regionsAvailabilityZones = region.getRegionV4Response().getAvailabilityZones();
            List<String> availabilityZones = new ArrayList<>();

            for (Entry<String, Collection<String>> regionAvailabilityZones : regionsAvailabilityZones.entrySet()) {
                availabilityZones.addAll(regionAvailabilityZones.getValue());
            }
            accessConfig.getRequest().setAvailabilityZone(availabilityZones.get(1));
        }

        Log.log(String.join(" ", " post AccessConfig"));
        accessConfig.setResponse(
                client.getCloudbreakClient()
                        .connectorV4Endpoint().getAccessConfigs(workspaceId, accessConfig.getRequest().getCredentialName(),
                        accessConfig.getRequest().getRegion(), accessConfig.getRequest().getPlatformVariant(),
                        accessConfig.getRequest().getAvailabilityZone()));
        Log.logJSON(" post AccessConfig response: ", accessConfig.getResponse());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        post(integrationTestContext, entity);
    }
}
