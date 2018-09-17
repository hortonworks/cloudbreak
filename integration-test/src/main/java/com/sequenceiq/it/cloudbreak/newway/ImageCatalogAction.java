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
                        .imageCatalogEndpoint().postPrivate(imageCatalogEntity.getRequest()));
        logJSON("Imagecatalog post request: ", imageCatalogEntity.getRequest());
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        ImageCatalogEntity imageCatalogEntity = (ImageCatalogEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        imageCatalogEntity.setResponse(
                client.getCloudbreakClient()
                        .imageCatalogEndpoint().getByName(imageCatalogEntity.getName(), false));
        logJSON("Imagecatalog get response: ", imageCatalogEntity.getResponse());
    }

    public static void getImagesByProviderFromImageCatalog(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        ImageCatalogEntity imageCatalogEntity = (ImageCatalogEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        imageCatalogEntity.setResponseByProvider(
                client.getCloudbreakClient()
                        .imageCatalogEndpoint().getImagesByProviderFromImageCatalog(imageCatalogEntity.getName(),
                        integrationTestContext.getContextParam(CLOUDPROVIDER, String.class)));
        logJSON("get response by provider from image catalog: ", imageCatalogEntity.getResponseByProvider());
    }

    public static void getImagesByProvider(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        ImageCatalogEntity imageCatalogEntity = (ImageCatalogEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        imageCatalogEntity.setResponseByProvider(
                client.getCloudbreakClient()
                        .imageCatalogEndpoint().getImagesByProvider(integrationTestContext.getContextParam(CLOUDPROVIDER, String.class)));
        logJSON("Imagecatalog get response by provider: ", imageCatalogEntity.getResponseByProvider());
    }

    public static void getRequestByName(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        ImageCatalogEntity imageCatalogEntity = (ImageCatalogEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        imageCatalogEntity.setRequest(
                client.getCloudbreakClient()
                        .imageCatalogEndpoint().getRequestfromName(imageCatalogEntity.getRequest().getName()));

        logJSON("Imagecatalog get response by provider: ", imageCatalogEntity.getRequest());
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        ImageCatalogEntity imageCatalogEntity = (ImageCatalogEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        imageCatalogEntity.setResponses(new HashSet<>(
                client.getCloudbreakClient().imageCatalogEndpoint().getPublics()));
        logJSON("Imagecatalog get response: ", imageCatalogEntity.getResponse());

    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity) {
        ImageCatalogEntity recipeEntity = (ImageCatalogEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        client.getCloudbreakClient().imageCatalogEndpoint()
                .deletePublic(recipeEntity.getName());
    }

    public static void putSetDefaultByName(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        ImageCatalogEntity imageCatalogEntity = (ImageCatalogEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        imageCatalogEntity.setResponse(
                client.getCloudbreakClient()
                        .imageCatalogEndpoint().putSetDefaultByName(imageCatalogEntity.getName()));
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