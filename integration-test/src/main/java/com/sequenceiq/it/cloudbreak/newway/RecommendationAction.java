package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;

public class RecommendationAction {

    private RecommendationAction() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) {
        RecommendationEntity recommendationEntity = (RecommendationEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);

        if (recommendationEntity.getRequest().getBlueprintId() == null
                && recommendationEntity.getRequest().getBlueprintName() == null) {
            Blueprint blueprint = integrationTestContext.getContextParam(BlueprintEntity.BLUEPRINT, Blueprint.class);
            if (blueprint != null && blueprint.getResponse() != null) {
                recommendationEntity.getRequest().setBlueprintId(
                        blueprint.getResponse().getId()
                );
            }
        }

        recommendationEntity.setResponse(
                client.getCloudbreakClient()
                        .connectorV1Endpoint().createRecommendation(
                        recommendationEntity.getRequest()));
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) {
        post(integrationTestContext, entity);
    }
}
