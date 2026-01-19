package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.ImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.UpdateImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.action.v4.imagecatalog.ImageCatalogGetImagesByNameAction;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;

public class ImageCatalogTest extends AbstractMockTest {

    private static final String IMG_CATALOG_URL = "https://cloudbreak-imagecatalog.s3.amazonaws.com/v2-prod-cb-image-catalog.json";

    private static final String IMAGECATALOG_NAME_WITH_SPECIAL_CHARS = "@#$%|:&*;";

    private static final String INVALID_IMAGECATALOG_JSON_URL = "https://rawgit.com/hortonworks/cloudbreak/master/integration-test/src/main/resources/"
            + "templates/imagecatalog_invalid.json";

    private static final String CB_DEFAULT_IMG_CATALOG_NAME = "cloudbreak-default";

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "image catalog valid URL",
            when = "calling create image catalog with that URL",
            then = "getting image catalog response so the creation success")
    public void testImageCatalogCreationWhenURLIsValidAndExists(MockedTestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName();

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(imageCatalogTestClient.createV4(), key(imgCatalogName))
                .when(imageCatalogTestClient.getV4(), key(imgCatalogName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "image catalog invalid URL but too short name",
            when = "calling create image catalog with that URL and name",
            then = "getting a BadRequestException")
    public void testImageCatalogCreationWhenNameIsTooShort(MockedTestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName().substring(0, 4);

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(IMG_CATALOG_URL)
                .whenException(imageCatalogTestClient.createV4(), BadRequestException.class,
                        expectedMessage(".*The length of the credential's name has to be in range of 5 to 100"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "image catalog invalid URL but too long name",
            when = "calling create image catalog with that URL and name",
            then = "getting a BadRequestException")
    public void testImageCatalogCreationWhenNameIsTooLong(MockedTestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName().concat(getLongNameGenerator().stringGenerator(100));

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(IMG_CATALOG_URL)
                .whenException(imageCatalogTestClient.createV4(), BadRequestException.class,
                        expectedMessage(".*The length of the credential's name has to be in range of 5 to 100"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "image catalog invalid name contains specific characters",
            when = "calling create image catalog with that URL and name",
            then = "getting a BadRequestException")
    public void testImageCatalogCreationWhenNameContainsSpecialCharacters(MockedTestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName().concat(IMAGECATALOG_NAME_WITH_SPECIAL_CHARS);

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(IMG_CATALOG_URL)
                .whenException(imageCatalogTestClient.createWithoutNameV4(), BadRequestException.class)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "image catalog invalid url",
            when = "calling create image catalog with that URL",
            then = "getting a BadRequestException")
    public void testImageCatalogCreationWhenTheCatalogURLIsInvalid(MockedTestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName();
        String invalidURL = "https:/google.com/imagecatalog";

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(invalidURL)
                .whenException(imageCatalogTestClient.createV4(), BadRequestException.class,
                        expectedMessage("A valid image catalog must be available on the given URL"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "image catalog valid url but does not contain a catalog",
            when = "calling create image catalog with that URL",
            then = "getting a BadRequestException")
    public void testImageCatalogCreationWhenTheCatalogURLPointsNotToAnImageCatalogJson(MockedTestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName();

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(IMG_CATALOG_URL + "/notanimagecatalog")
                .whenException(imageCatalogTestClient.createV4(), BadRequestException.class,
                        expectedMessage(".*A valid image catalog must be available on the given URL.*"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "image catalog valid url but the JSON is invalid",
            when = "calling create image catalog with that URL",
            then = "getting a BadRequestException")
    public void testImageCatalogCreationWhenTheCatalogURLPointsToAnInvalidImageCatalogJson(MockedTestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName();

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(INVALID_IMAGECATALOG_JSON_URL)
                .whenException(imageCatalogTestClient.createV4(), BadRequestException.class,
                        expectedMessage("A valid image catalog must be available on the given URL"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "image catalog deletion",
            when = "calling delete on the existing one",
            then = "the deletion was success")
    public void testImageCatalogDeletion(MockedTestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName();
        String catalogKey = resourcePropertyProvider().getName();

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(imageCatalogTestClient.createV4(), key(catalogKey))
                .when(imageCatalogTestClient.deleteV4(), key(catalogKey))
                .whenException(imageCatalogTestClient.getImagesByNameV4(), NotFoundException.class, expectedMessage("not found"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "image catalog get the default catalog with MOCK provider",
            when = "calling get on the default",
            then = "the response contains MOCK images")
    public void testGetImageCatalogWhenCatalogContainsTheRequestedProvider(MockedTestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName();

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(imageCatalogTestClient.createV4(), key(imgCatalogName))
                .when(imageCatalogTestClient.getImagesByNameV4(), key(imgCatalogName))
                .then((testContext1, entity, cloudbreakClient) -> {
                    ImagesV4Response catalog = entity.getResponseByProvider();
                    if (catalog.getBaseImages().isEmpty() && catalog.getCdhImages().isEmpty()) {
                        throw new IllegalArgumentException("The Images response should contain results for MOCK provider.");
                    }
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "image catalog get the default catalog with AWS provider but catalog does not contain AWS entry",
            when = "calling get on the default",
            then = "the response does not contains AWS images")
    public void testGetImageCatalogWhenCatalogDoesNotContainTheRequestedProvider(MockedTestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName();

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(imageCatalogTestClient.createV4(), key(imgCatalogName))
                .when(new ImageCatalogGetImagesByNameAction(CloudPlatform.AWS), key(imgCatalogName))
                .then((testContext1, entity, cloudbreakClient) -> {
                    ImagesV4Response catalog = entity.getResponseByProvider();
                    if (!catalog.getBaseImages().isEmpty()) {
                        throw new IllegalArgumentException("The Images response should NOT contain results for AWS provider.");
                    }
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "image catalog get with a non existing name",
            when = "calling get with that name",
            then = "getting a NotFoundException")
    public void testGetImageCatalogWhenTheSpecifiedCatalogDoesNotExistWithName(MockedTestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName();

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(getImageCatalogMockServerSetup().getImageCatalogUrl())
                .whenException(imageCatalogTestClient.getImagesByNameV4(), NotFoundException.class)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "image catalog get with AWS provider if exist in catalog",
            when = "calling get AWS provider",
            then = "getting a list with AWS specific images")
    public void testGetImageCatalogWhenDefaultCatalogContainsTheRequestedProvider(MockedTestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName();

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(imageCatalogTestClient.createV4(), key(imgCatalogName))
                .when(imageCatalogTestClient.getImagesFromDefaultCatalog(CloudPlatform.AWS), key(imgCatalogName))
                .then((testContext1, entity, cloudbreakClient) -> {
                    ImagesV4Response catalog = entity.getResponseByProvider();
                    if (catalog.getBaseImages().isEmpty() && catalog.getCdhImages().isEmpty()) {
                        throw new IllegalArgumentException("The Images response should contain results for AWS provider.");
                    }
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "image catalog get with AWS provider if exist in catalog",
            when = "calling get AWS provider",
            then = "getting a list with AWS specific images")
    public void testGetImageCatalogWhenDefaultCatalogDoesNotContainTheRequestedProvider(MockedTestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName();

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(imageCatalogTestClient.createV4(), key(imgCatalogName))
                .when(imageCatalogTestClient.getImagesFromDefaultCatalog(), key(imgCatalogName))
                .then((testContext1, entity, cloudbreakClient) -> {
                    ImagesV4Response catalog = entity.getResponseByProvider();
                    if (!catalog.getBaseImages().isEmpty()) {
                        throw new IllegalArgumentException("The Images response should NOT contain results for MOCK provider.");
                    }
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "image catalog get request",
            when = "calling get request on catalog endpoint",
            then = "getting back the catalog request")
    public void testGetImageCatalogsRequestFromExistingImageCatalog(MockedTestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName();

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(imageCatalogTestClient.createV4(), key(imgCatalogName))
                .select(ImageCatalogTestDto::getRequest, key(imgCatalogName))
                .when((testContext1, entity, cloudbreakClient) -> {
                    ImageCatalogV4Request request = cloudbreakClient
                            .getDefaultClient(testContext)
                            .imageCatalogV4Endpoint()
                            .getRequest(cloudbreakClient.getWorkspaceId(), imgCatalogName);
                    entity.setRequest(request);
                    return entity;
                })
                .then((testContext1, entity, cloudbreakClient) -> {
                    ImageCatalogV4Request imgCatReq = testContext1.getSelected(imgCatalogName);
                    if (entity.getRequest().equals(imgCatReq)) {
                        throw new IllegalArgumentException("The requests are not identical.");
                    }
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "image catalog update request",
            when = "calling update request with new url",
            then = "the image catalog list should contains the new url")
    public void testUpdateImageCatalog(MockedTestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName();

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(imageCatalogTestClient.createV4(), key(imgCatalogName))
                .select(ImageCatalogTestDto::getResponse, key(imgCatalogName))
                .when((testContext1, entity, cloudbreakClient) -> {
                    ImageCatalogV4Response originalResponse = entity.getResponse();
                    UpdateImageCatalogV4Request updateRequest = new UpdateImageCatalogV4Request();
                    updateRequest.setCrn(originalResponse.getCrn());
                    updateRequest.setName(originalResponse.getName());
                    updateRequest.setUrl(IMG_CATALOG_URL);

                    ImageCatalogV4Response updateResponse = cloudbreakClient
                            .getDefaultClient(testContext)
                            .imageCatalogV4Endpoint()
                            .update(cloudbreakClient.getWorkspaceId(), updateRequest);
                    entity.setResponse(updateResponse);
                    return entity;
                }, key(imgCatalogName))
                .then((testContext1, entity, cloudbreakClient) -> {
                    ImageCatalogV4Response originalRepsonse = testContext1.getSelected(imgCatalogName);
                    if (originalRepsonse.getUrl().equals(entity.getResponse().getUrl())) {
                        throw new IllegalArgumentException("The catalog URL should not be the same after update.");
                    }
                    return entity;
                }, key(imgCatalogName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "image catalog list",
            when = "calling list all the catalog",
            then = "getting back the account related catalogs")
    public void testGetListOfImageCatalogs(MockedTestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName();

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(CB_DEFAULT_IMG_CATALOG_NAME)
                .when((testContext1, entity, cloudbreakClient) -> {
                    ImageCatalogV4Responses list = cloudbreakClient
                            .getDefaultClient(testContext)
                            .imageCatalogV4Endpoint()
                            .list(cloudbreakClient.getWorkspaceId(), false);
                    return entity;
                })
                .validate();
    }
}
