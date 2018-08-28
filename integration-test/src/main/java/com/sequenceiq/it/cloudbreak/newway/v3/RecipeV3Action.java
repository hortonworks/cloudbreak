package com.sequenceiq.it.cloudbreak.newway.v3;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Entity;
import com.sequenceiq.it.cloudbreak.newway.RecipeEntity;

public class RecipeV3Action {

    private RecipeV3Action() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) {
        RecipeEntity recipeEntity = (RecipeEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long orgId = integrationTestContext.getContextParam(CloudbreakTest.ORGANIZATION_ID, Long.class);
        recipeEntity.setResponse(
                client.getCloudbreakClient()
                        .recipeV3Endpoint().createInOrganization(orgId, recipeEntity.getRequest()));
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) {
        RecipeEntity recipeEntity = (RecipeEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long orgId = integrationTestContext.getContextParam(CloudbreakTest.ORGANIZATION_ID, Long.class);
        recipeEntity.setResponse(
                client.getCloudbreakClient()
                        .recipeV3Endpoint()
                        .getByNameInOrganization(orgId, recipeEntity.getName()));
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) {
        RecipeEntity recipeEntity = (RecipeEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long orgId = integrationTestContext.getContextParam(CloudbreakTest.ORGANIZATION_ID, Long.class);
        recipeEntity.setResponses(
                client.getCloudbreakClient().recipeV3Endpoint().listByOrganization(orgId));

    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity) {
        RecipeEntity recipeEntity = (RecipeEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long orgId = integrationTestContext.getContextParam(CloudbreakTest.ORGANIZATION_ID, Long.class);
        client.getCloudbreakClient().recipeV3Endpoint()
                .deleteInOrganization(orgId, recipeEntity.getName());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) {
        post(integrationTestContext, entity);
    }

    public static void createDeleteInGiven(IntegrationTestContext integrationTestContext, Entity entity) {
        post(integrationTestContext, entity);
        delete(integrationTestContext, entity);
    }
}
