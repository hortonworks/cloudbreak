package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class RecommendationAction {

    private RecommendationAction() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        RecommendationEntity recommendationEntity = (RecommendationEntity) entity;
        CloudbreakClient client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);

        Blueprint blueprint = integrationTestContext.getContextParam(BlueprintEntity.BLUEPRINT, Blueprint.class);
        if (blueprint != null && blueprint.getResponse() != null) {
            recommendationEntity.getRequest().setBlueprintId(blueprint.getResponse().getId());
        }

        Credential credential = Credential.getTestContextCredential().apply(integrationTestContext);
        if (credential != null && recommendationEntity.getRequest().getCredentialId() == null) {
            recommendationEntity.getRequest().setCredentialId(credential.getResponse().getId());
        }

        Region region = Region.getTestContextRegion().apply(integrationTestContext);
        if (region != null && recommendationEntity.getRequest().getRegion() == null) {
            recommendationEntity.getRequest().setRegion(region.getRegionResponse().getDefaultRegion());
        }
        if (region != null && recommendationEntity.getRequest().getAvailabilityZone() == null) {
            Map<String, Collection<String>> regionsAvailabilityZones = region.getRegionResponse().getAvailabilityZones();
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
                        .connectorV1Endpoint().createRecommendation(
                        recommendationEntity.getRequest()));
        Log.logJSON(" post Recommendations response: ", recommendationEntity.getResponse());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        post(integrationTestContext, entity);
    }
}
