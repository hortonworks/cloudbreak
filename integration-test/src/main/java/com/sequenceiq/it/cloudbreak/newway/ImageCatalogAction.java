package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants.CLOUDPROVIDER;
import static com.sequenceiq.it.cloudbreak.newway.ImageCatalogEntity.IMAGE_CATALOG_URL;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import java.util.HashSet;

import com.sequenceiq.it.IntegrationTestContext;

public class ImageCatalogAction {

    private ImageCatalogAction() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        ImageCatalogEntity imageCatalogEntity = (ImageCatalogEntity) entity;
        CloudbreakClient client;
        String imageCatalogUrl = integrationTestContext.getContextParam(IMAGE_CATALOG_URL, String.class);
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);

        if (imageCatalogUrl != null && imageCatalogEntity.getRequest().getUrl() == null) {
            imageCatalogEntity.getRequest().setUrl(imageCatalogUrl);
        }

        imageCatalogEntity.setResponse(
                client.getCloudbreakClient()
                        .imageCatalogV4Endpoint().create(client.getWorkspaceId(), imageCatalogEntity.getRequest()));
        logJSON("Imagecatalog post request: ", imageCatalogEntity.getRequest());
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        ImageCatalogEntity imageCatalogEntity = (ImageCatalogEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        imageCatalogEntity.setResponse(
                client.getCloudbreakClient()
                        .imageCatalogV4Endpoint().get(client.getWorkspaceId(), imageCatalogEntity.getName(), Boolean.FALSE));
        logJSON("Imagecatalog get response: ", imageCatalogEntity.getResponse());
    }

    public static void getImagesByProviderFromImageCatalog(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        ImageCatalogEntity imageCatalogEntity = (ImageCatalogEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        String provider = integrationTestContext.getContextParam(CLOUDPROVIDER, String.class);
        imageCatalogEntity.setResponseByProvider(
                client.getCloudbreakClient()
                        .imageCatalogV4Endpoint().getImagesByName(client.getWorkspaceId(), imageCatalogEntity.getName(), null, provider));
        logJSON("get response by provider from image catalog: ", imageCatalogEntity.getResponseByProvider());
    }

    public static void getImagesByProvider(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        ImageCatalogEntity imageCatalogEntity = (ImageCatalogEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        String provider = integrationTestContext.getContextParam(CLOUDPROVIDER, String.class);
        imageCatalogEntity.setResponseByProvider(
                client.getCloudbreakClient()
                        .imageCatalogV4Endpoint().getImages(client.getWorkspaceId(), null, provider));
        logJSON("Imagecatalog get response by provider: ", imageCatalogEntity.getResponseByProvider());
    }

    public static void getRequestByName(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        ImageCatalogEntity imageCatalogEntity = (ImageCatalogEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        imageCatalogEntity.setRequest(
                client.getCloudbreakClient()
                        .imageCatalogV4Endpoint().getRequest(client.getWorkspaceId(), imageCatalogEntity.getRequest().getName()));

        logJSON("Imagecatalog get response by provider: ", imageCatalogEntity.getRequest());
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        ImageCatalogEntity imageCatalogEntity = (ImageCatalogEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        imageCatalogEntity.setResponses(new HashSet<>(
                client.getCloudbreakClient().imageCatalogV4Endpoint().list(client.getWorkspaceId()).getResponses()));
        logJSON("Imagecatalog get response: ", imageCatalogEntity.getResponse());

    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity) {
        ImageCatalogEntity recipeEntity = (ImageCatalogEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        client.getCloudbreakClient().imageCatalogV4Endpoint()
                .delete(client.getWorkspaceId(), recipeEntity.getName());
    }

    public static void putSetDefaultByName(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        ImageCatalogEntity imageCatalogEntity = (ImageCatalogEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        imageCatalogEntity.setResponse(
                client.getCloudbreakClient()
                        .imageCatalogV4Endpoint().setDefault(client.getWorkspaceId(), imageCatalogEntity.getName()));
        logJSON("Imagecatalog get response: ", imageCatalogEntity.getResponse());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        post(integrationTestContext, entity);
    }

    public static void createDeleteInGiven(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        post(integrationTestContext, entity);
        delete(integrationTestContext, entity);
    }

    public static void createAsDefaultInGiven(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        post(integrationTestContext, entity);
        putSetDefaultByName(integrationTestContext, entity);
    }
}