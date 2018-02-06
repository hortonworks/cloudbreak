package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;

public class RecipeAction {

    private RecipeAction() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) {
        RecipeEntity recipeEntity = (RecipeEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        recipeEntity.setResponse(
                client.getCloudbreakClient()
                        .recipeEndpoint().postPrivate(recipeEntity.getRequest()));
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) {
        RecipeEntity recipeEntity = (RecipeEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        recipeEntity.setResponse(
                client.getCloudbreakClient()
                        .recipeEndpoint()
                        .getPrivate(recipeEntity.getName()));
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) {
        RecipeEntity recipeEntity = (RecipeEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        recipeEntity.setResponses(
                client.getCloudbreakClient().recipeEndpoint().getPrivates());

    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity) {
        RecipeEntity recipeEntity = (RecipeEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        client.getCloudbreakClient().recipeEndpoint()
                .deletePrivate(recipeEntity.getName());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) {
        post(integrationTestContext, entity);
    }

    public static void createDeleteInGiven(IntegrationTestContext integrationTestContext, Entity entity) {
        post(integrationTestContext, entity);
        delete(integrationTestContext, entity);
    }
}
