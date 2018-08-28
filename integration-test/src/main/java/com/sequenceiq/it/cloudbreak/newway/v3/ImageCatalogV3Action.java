package com.sequenceiq.it.cloudbreak.newway.v3;

import static com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants.CLOUDPROVIDER;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import java.util.HashSet;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Entity;
import com.sequenceiq.it.cloudbreak.newway.ImageCatalogEntity;

public class ImageCatalogV3Action {

    private ImageCatalogV3Action() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        ImageCatalogEntity imageCatalogEntity = (ImageCatalogEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long orgId = integrationTestContext.getContextParam(CloudbreakTest.ORGANIZATION_ID, Long.class);
        imageCatalogEntity.setResponse(
                client.getCloudbreakClient()
                        .imageCatalogV3Endpoint().createInOrganization(orgId, imageCatalogEntity.getRequest()));
        logJSON("Imagecatalog post request: ", imageCatalogEntity.getRequest());
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        ImageCatalogEntity imageCatalogEntity = (ImageCatalogEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long orgId = integrationTestContext.getContextParam(CloudbreakTest.ORGANIZATION_ID, Long.class);
        imageCatalogEntity.setResponse(
                client.getCloudbreakClient()
                        .imageCatalogV3Endpoint().getByNameInOrganization(orgId, imageCatalogEntity.getName(), false));
        logJSON("Imagecatalog get response: ", imageCatalogEntity.getResponse());
    }

    public static void getImagesByProviderFromImageCatalog(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        ImageCatalogEntity imageCatalogEntity = (ImageCatalogEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long orgId = integrationTestContext.getContextParam(CloudbreakTest.ORGANIZATION_ID, Long.class);
        imageCatalogEntity.setResponseByProvider(
                client.getCloudbreakClient()
                        .imageCatalogV3Endpoint().getImagesByProviderFromImageCatalogInOrganization(orgId, imageCatalogEntity.getName(),
                        integrationTestContext.getContextParam(CLOUDPROVIDER, String.class)));
        logJSON("get response by provider from image catalog: ", imageCatalogEntity.getResponseByProvider());
    }

    public static void getImagesByProvider(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        ImageCatalogEntity imageCatalogEntity = (ImageCatalogEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long orgId = integrationTestContext.getContextParam(CloudbreakTest.ORGANIZATION_ID, Long.class);
        imageCatalogEntity.setResponseByProvider(
                client.getCloudbreakClient()
                        .imageCatalogV3Endpoint().getImagesByProvider(orgId, integrationTestContext.getContextParam(CLOUDPROVIDER, String.class)));
        logJSON("Imagecatalog get response by provider: ", imageCatalogEntity.getResponseByProvider());
    }

    public static void getRequestByName(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        ImageCatalogEntity imageCatalogEntity = (ImageCatalogEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long orgId = integrationTestContext.getContextParam(CloudbreakTest.ORGANIZATION_ID, Long.class);
        imageCatalogEntity.setRequestByName(
                client.getCloudbreakClient()
                        .imageCatalogV3Endpoint().getRequestFromName(orgId, imageCatalogEntity.getRequest().getName()));

        logJSON("Imagecatalog get response by provider: ", imageCatalogEntity.getRequestByName());
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        ImageCatalogEntity imageCatalogEntity = (ImageCatalogEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long orgId = integrationTestContext.getContextParam(CloudbreakTest.ORGANIZATION_ID, Long.class);
        imageCatalogEntity.setResponses(new HashSet<>(
                client.getCloudbreakClient().imageCatalogV3Endpoint().listByOrganization(orgId)));
        logJSON("Imagecatalog get response: ", imageCatalogEntity.getResponse());

    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity) {
        ImageCatalogEntity recipeEntity = (ImageCatalogEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long orgId = integrationTestContext.getContextParam(CloudbreakTest.ORGANIZATION_ID, Long.class);
        client.getCloudbreakClient().imageCatalogV3Endpoint()
                .deleteInOrganization(orgId, recipeEntity.getName());
    }

    public static void putSetDefaultByName(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        ImageCatalogEntity imageCatalogEntity = (ImageCatalogEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long orgId = integrationTestContext.getContextParam(CloudbreakTest.ORGANIZATION_ID, Long.class);
        imageCatalogEntity.setResponse(
                client.getCloudbreakClient()
                        .imageCatalogV3Endpoint().putSetDefaultByNameInOrganization(orgId, imageCatalogEntity.getName()));
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
