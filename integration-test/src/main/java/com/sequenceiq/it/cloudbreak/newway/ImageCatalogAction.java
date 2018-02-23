package com.sequenceiq.it.cloudbreak.newway;

import java.util.HashSet;

import com.sequenceiq.it.IntegrationTestContext;

public class ImageCatalogAction {

    private ImageCatalogAction() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) {
        ImageCatalogEntity imageCatalogEntity = (ImageCatalogEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        imageCatalogEntity.setResponse(
                client.getCloudbreakClient()
                        .imageCatalogEndpoint().postPrivate(imageCatalogEntity.getRequest()));
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) {
        ImageCatalogEntity imageCatalogEntity = (ImageCatalogEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        imageCatalogEntity.setResponse(
                client.getCloudbreakClient()
                        .imageCatalogEndpoint().getPublicByName(imageCatalogEntity.getName(), false));
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) {
        ImageCatalogEntity recipeEntity = (ImageCatalogEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        recipeEntity.setResponses(new HashSet<>(
                client.getCloudbreakClient().imageCatalogEndpoint().getPublics()));

    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity) {
        ImageCatalogEntity recipeEntity = (ImageCatalogEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        client.getCloudbreakClient().imageCatalogEndpoint()
                .deletePublic(recipeEntity.getName());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) {
        post(integrationTestContext, entity);
    }
}
