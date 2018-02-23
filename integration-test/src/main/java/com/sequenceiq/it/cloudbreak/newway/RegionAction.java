package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;

public class RegionAction {

    private RegionAction() {
    }

    public static void getPlatformRegions(IntegrationTestContext integrationTestContext, Entity entity) {
        Region region = (Region) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        region.setPlatformRegionsResponse(client.getCloudbreakClient().connectorV1Endpoint().getRegions());
    }

    public static void getRegionAvByType(IntegrationTestContext integrationTestContext, Entity entity) {
        Region region = (Region) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        region.setRegionAvResponse(client.getCloudbreakClient().connectorV1Endpoint().getRegionAvByType(region.getType()));
    }

    public static void getRegionRByType(IntegrationTestContext integrationTestContext, Entity entity) {
        Region region = (Region) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        region.setRegionRResponse(client.getCloudbreakClient().connectorV1Endpoint().getRegionRByType(region.getType()));
    }
}
