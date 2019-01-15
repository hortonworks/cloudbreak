package com.sequenceiq.it.cloudbreak.newway.v4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.Blueprint;
import com.sequenceiq.it.cloudbreak.newway.BlueprintEntity;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.Entity;
import com.sequenceiq.it.cloudbreak.newway.RecommendationEntity;
import com.sequenceiq.it.cloudbreak.newway.Region;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

public class RecommendationV4Action {

    private RecommendationV4Action() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        RecommendationEntity recommendationEntity = (RecommendationEntity) entity;
        CloudbreakClient client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);

        Blueprint blueprint = integrationTestContext.getContextParam(BlueprintEntity.BLUEPRINT, Blueprint.class);
        if (blueprint != null && blueprint.getResponse() != null) {
            recommendationEntity.getRequest().setBlueprintName(blueprint.getResponse().getName());
        }

        Credential credential = Credential.getTestContextCredential().apply(integrationTestContext);
        if (credential != null && recommendationEntity.getRequest().getCredentialName() == null) {
            recommendationEntity.getRequest().setCredentialName(credential.getResponse().getName());
        }

        Region region = Region.getTestContextRegion().apply(integrationTestContext);
        if (region != null && recommendationEntity.getRequest().getRegion() == null) {
            recommendationEntity.getRequest().setRegion(region.getRegionV4Response().getDefaultRegion());
        }
        if (region != null && recommendationEntity.getRequest().getAvailabilityZone() == null) {
            Map<String, Collection<String>> regionsAvailabilityZones = region.getRegionV4Response().getAvailabilityZones();
            List<String> availabilityZones = new ArrayList<>();

            for (Entry<String, Collection<String>> regionAvailabilityZones : regionsAvailabilityZones.entrySet()) {
                availabilityZones.addAll(regionAvailabilityZones.getValue());
            }
            recommendationEntity.getRequest().setAvailabilityZone(availabilityZones.get(1));
        }

        Log.log(String.join(" ", " post Recommendations to",
                recommendationEntity.getRequest().getCredentialName(), "credential and to",
                recommendationEntity.getRequest().getBlueprintName(), "blueprint",
                recommendationEntity.getRequest().getRegion(), "region",
                recommendationEntity.getRequest().getAvailabilityZone(), "availability zone. "));
        recommendationEntity.setResponse(
                client.getCloudbreakClient()
                        .blueprintV4Endpoint()
                        .createRecommendation(workspaceId, recommendationEntity.getRequest()));
        Log.logJSON(" post Recommendations response: ", recommendationEntity.getResponse());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        post(integrationTestContext, entity);
    }
}
