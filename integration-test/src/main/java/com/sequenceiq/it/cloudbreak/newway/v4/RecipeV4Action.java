package com.sequenceiq.it.cloudbreak.newway.v4;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeViewV4Response;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Entity;
import com.sequenceiq.it.cloudbreak.newway.RecipeEntity;

public class RecipeV4Action {

    private RecipeV4Action() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) {
        RecipeEntity recipeEntity = (RecipeEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        recipeEntity.setResponse(
                client.getCloudbreakClient()
                        .recipeV4Endpoint().post(workspaceId, recipeEntity.getRequest()));
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) {
        RecipeEntity recipeEntity = (RecipeEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        recipeEntity.setResponse(
                client.getCloudbreakClient()
                        .recipeV4Endpoint()
                        .get(workspaceId, recipeEntity.getName()));
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) {
        RecipeEntity recipeEntity = (RecipeEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Collection<RecipeViewV4Response> recipes = client.getCloudbreakClient().recipeV4Endpoint().list(workspaceId).getResponses();
        Set<RecipeV4Response> detailedRecipes = new HashSet<>();
        recipes.stream().forEach(
                recipe -> detailedRecipes.add(client.getCloudbreakClient().recipeV4Endpoint().get(workspaceId, recipe.getName())));
        recipeEntity.setResponses(detailedRecipes);
    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity) {
        RecipeEntity recipeEntity = (RecipeEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        client.getCloudbreakClient().recipeV4Endpoint()
                .delete(workspaceId, recipeEntity.getName());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) {
        post(integrationTestContext, entity);
    }

    public static void createDeleteInGiven(IntegrationTestContext integrationTestContext, Entity entity) {
        post(integrationTestContext, entity);
        delete(integrationTestContext, entity);
    }
}
