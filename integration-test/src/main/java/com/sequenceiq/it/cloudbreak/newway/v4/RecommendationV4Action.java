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
import com.sequenceiq.it.cloudbreak.newway.Region;
import com.sequenceiq.it.cloudbreak.newway.dto.RecommendationTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

public class RecommendationV4Action {

    private RecommendationV4Action() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        RecommendationTestDto recommendationEntity = (RecommendationTestDto) entity;
        CloudbreakClient client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);

        BlueprintTestDto blueprint = integrationTestContext.getContextParam(BlueprintTestDto.BLUEPRINT,
                BlueprintTestDto.class);
        if (blueprint != null && blueprint.getResponse() != null) {
            recommendationEntity.withAvailabilityZone(blueprint.getResponse().getName());
        }

        Credential credential = Credential.getTestContextCredential().apply(integrationTestContext);
        if (credential != null) {
            recommendationEntity.withCredentialName(credential.getResponse().getName());
        }

        Region region = Region.getTestContextRegion().apply(integrationTestContext);
        if (region != null) {
            recommendationEntity.withRegion(region.getRegionV4Response().getDefaultRegion());
        }
        if (region != null) {
            Map<String, Collection<String>> regionsAvailabilityZones = region.getRegionV4Response().getAvailabilityZones();
            List<String> availabilityZones = new ArrayList<>();

            for (Entry<String, Collection<String>> regionAvailabilityZones : regionsAvailabilityZones.entrySet()) {
                availabilityZones.addAll(regionAvailabilityZones.getValue());
            }
            recommendationEntity.withAvailabilityZone(availabilityZones.get(1));
        }

        Log.log(String.join(" ", " post Recommendations to",
                recommendationEntity.getCredentialName(), "credential and to",
                recommendationEntity.getBlueprintName(), "blueprint",
                recommendationEntity.getRegion(), "region",
                recommendationEntity.getAvailabilityZone(), "availability zone. "));
        recommendationEntity.setResponse(
                client.getCloudbreakClient()
                        .blueprintV4Endpoint()
                        .createRecommendation(workspaceId, recommendationEntity.getBlueprintName(), recommendationEntity.getCredentialName(),
                                recommendationEntity.getRegion(), null, recommendationEntity.getAvailabilityZone()));
        Log.logJSON(" post Recommendations response: ", recommendationEntity.getResponse());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        post(integrationTestContext, entity);
    }
}
