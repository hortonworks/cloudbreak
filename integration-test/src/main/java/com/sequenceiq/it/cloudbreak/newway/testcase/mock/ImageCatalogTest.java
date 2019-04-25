package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;
import static java.lang.String.format;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.ImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.UpdateImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.it.cloudbreak.newway.action.v4.imagecatalog.ImageCatalogGetImagesByNameAction;
import com.sequenceiq.it.cloudbreak.newway.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class ImageCatalogTest extends AbstractIntegrationTest {

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
    public void testImageCatalogCreationWhenURLIsValidAndExists(TestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(imageCatalogTestClient.createV4(), key(imgCatalogName))
                .when(imageCatalogTestClient.getV4(Boolean.FALSE), key(imgCatalogName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "image catalog invalid URL but too short name",
            when = "calling create image catalog with that URL and name",
            then = "getting a BadRequestException")
    public void testImageCatalogCreationWhenNameIsTooShort(TestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName().substring(0, 4);

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(IMG_CATALOG_URL)
                .when(imageCatalogTestClient.createV4(), key(imgCatalogName))
                .expect(BadRequestException.class, key(imgCatalogName)
                        .withExpectedMessage(".*The length of the credential's name has to be in range of 5 to 100"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "image catalog invalid URL but too long name",
            when = "calling create image catalog with that URL and name",
            then = "getting a BadRequestException")
    public void testImageCatalogCreationWhenNameIsTooLong(TestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName().concat(getLongNameGenerator().stringGenerator(100));

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(IMG_CATALOG_URL)
                .when(imageCatalogTestClient.createV4(), key(imgCatalogName))
                .expect(BadRequestException.class, key(imgCatalogName)
                        .withExpectedMessage(".*The length of the credential's name has to be in range of 5 to 100"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "image catalog invalid name contains specific characters",
            when = "calling create image catalog with that URL and name",
            then = "getting a BadRequestException")
    public void testImageCatalogCreationWhenNameContainsSpecialCharacters(TestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName().concat(IMAGECATALOG_NAME_WITH_SPECIAL_CHARS);

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(IMG_CATALOG_URL)
                .when(imageCatalogTestClient.createWithoutNameV4(), key(imgCatalogName))
                .expect(BadRequestException.class, key(imgCatalogName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "image catalog invalid url",
            when = "calling create image catalog with that URL",
            then = "getting a BadRequestException")
    public void testImageCatalogCreationWhenTheCatalogURLIsInvalid(TestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName();
        String invalidURL = "https:/google.com/imagecatalog";

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(invalidURL)
                .when(imageCatalogTestClient.createV4(), key(imgCatalogName))
                .expect(BadRequestException.class, key(imgCatalogName)
                        .withExpectedMessage("A valid image catalog must be available on the given URL"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "image catalog valid url but does not contain a catalog",
            when = "calling create image catalog with that URL",
            then = "getting a BadRequestException")
    public void testImageCatalogCreationWhenTheCatalogURLPointsNotToAnImageCatalogJson(TestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName();

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(IMG_CATALOG_URL + "/notanimagecatalog")
                .when(imageCatalogTestClient.createV4(), key(imgCatalogName))
                .expect(BadRequestException.class, key(imgCatalogName)
                        .withExpectedMessage(".*A valid image catalog must be available on the given URL.*"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "image catalog valid url but the JSON is invalid",
            when = "calling create image catalog with that URL",
            then = "getting a BadRequestException")
    public void testImageCatalogCreationWhenTheCatalogURLPointsToAnInvalidImageCatalogJson(TestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName();

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(INVALID_IMAGECATALOG_JSON_URL)
                .when(imageCatalogTestClient.createV4(), key(imgCatalogName))
                .expect(BadRequestException.class, key(imgCatalogName)
                        .withExpectedMessage("A valid image catalog must be available on the given URL"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "image catalog in the database",
            when = "calling delete on the existing one and create a new one with the same name",
            then = "getting an image catalog response so the request was valid")
    public void testImageCatalogCreationWhenCatalogWithTheSameNameDeletedRightBeforeCreation(TestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(imageCatalogTestClient.createV4(), key(imgCatalogName))
                .select(imgCatalog -> imgCatalog.getResponse().getId(), key(imgCatalogName))
                .when(imageCatalogTestClient.deleteV4(), key(imgCatalogName))
                .when(imageCatalogTestClient.createV4(), key(imgCatalogName))
                .when(imageCatalogTestClient.getV4(Boolean.FALSE), key(imgCatalogName))
                .then((testContext1, entity, cloudbreakClient) -> {
                    Long firstPostEntityId = testContext1.getSelected(imgCatalogName);
                    if (entity.getResponse().getId().equals(firstPostEntityId)) {
                        throw new IllegalArgumentException("The re-created ImageCatalog should have a different id.");
                    }
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "image catalog deletion",
            when = "calling delete on the existing one",
            then = "the deletion was success")
    public void testImageCatalogDeletion(TestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName();
        String catalogKey = resourcePropertyProvider().getName();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(imageCatalogTestClient.createV4(), key(catalogKey))
                .when(imageCatalogTestClient.deleteV4(), key(catalogKey))
                .when(imageCatalogTestClient.getImagesByNameV4(), key(imgCatalogName))
                .expect(ForbiddenException.class, key(imgCatalogName)
                        .withExpectedMessage("catalog does not exist or does not belongs to your account"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "image catalog get the default catalog with MOCK provider",
            when = "calling get on the default",
            then = "the response contains MOCK images")
    public void testGetImageCatalogWhenCatalogContainsTheRequestedProvider(TestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(imageCatalogTestClient.createV4(), key(imgCatalogName))
                .when(imageCatalogTestClient.getImagesByNameV4(), key(imgCatalogName))
                .then((testContext1, entity, cloudbreakClient) -> {
                    ImagesV4Response catalog = entity.getResponseByProvider();
                    if (catalog.getBaseImages().isEmpty() && catalog.getHdpImages().isEmpty() && catalog.getHdfImages().isEmpty()) {
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
    public void testGetImageCatalogWhenCatalogDoesNotContainTheRequestedProvider(TestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(imageCatalogTestClient.createV4(), key(imgCatalogName))
                .when(new ImageCatalogGetImagesByNameAction(CloudPlatform.AWS), key(imgCatalogName))
                .then((testContext1, entity, cloudbreakClient) -> {
                    ImagesV4Response catalog = entity.getResponseByProvider();
                    if (!(catalog.getBaseImages().isEmpty() && catalog.getHdpImages().isEmpty() && catalog.getHdfImages().isEmpty())) {
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
            then = "getting a ForbiddenException")
    public void testGetImageCatalogWhenTheSpecifiedCatalogDoesNotExistWithName(TestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(imageCatalogTestClient.getImagesByNameV4(), key(imgCatalogName))
                .expect(ForbiddenException.class, key(imgCatalogName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "image catalog get with AWS provider if exist in catalog",
            when = "calling get AWS provider",
            then = "getting a list with AWS specific images")
    public void testGetImageCatalogWhenDefaultCatalogContainsTheRequestedProvider(TestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(imageCatalogTestClient.createV4(), key(imgCatalogName))
                .when(imageCatalogTestClient.getImagesFromDefaultCatalog(CloudPlatform.AWS), key(imgCatalogName))
                .then((testContext1, entity, cloudbreakClient) -> {
                    ImagesV4Response catalog = entity.getResponseByProvider();
                    if (catalog.getBaseImages().isEmpty() && catalog.getHdpImages().isEmpty() && catalog.getHdfImages().isEmpty()) {
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
    public void testGetImageCatalogWhenDefaultCatalogDoesNotContainTheRequestedProvider(TestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(imageCatalogTestClient.createV4(), key(imgCatalogName))
                .when(imageCatalogTestClient.getImagesFromDefaultCatalog(), key(imgCatalogName))
                .then((testContext1, entity, cloudbreakClient) -> {
                    ImagesV4Response catalog = entity.getResponseByProvider();
                    if (!(catalog.getBaseImages().isEmpty() && catalog.getHdpImages().isEmpty() && catalog.getHdfImages().isEmpty())) {
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
    public void testGetImageCatalogsRequestFromExistingImageCatalog(TestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(imageCatalogTestClient.createV4(), key(imgCatalogName))
                .select(ImageCatalogTestDto::getRequest, key(imgCatalogName))
                .when((testContext1, entity, cloudbreakClient) -> {
                    ImageCatalogV4Request request = cloudbreakClient
                            .getCloudbreakClient()
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
    public void testUpdateImageCatalog(TestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(imageCatalogTestClient.createV4(), key(imgCatalogName))
                .select(ImageCatalogTestDto::getResponse, key(imgCatalogName))
                .when((testContext1, entity, cloudbreakClient) -> {
                    ImageCatalogV4Response originalResponse = entity.getResponse();
                    UpdateImageCatalogV4Request updateRequest = new UpdateImageCatalogV4Request();
                    updateRequest.setId(originalResponse.getId());
                    updateRequest.setName(originalResponse.getName());
                    updateRequest.setUrl(IMG_CATALOG_URL);

                    ImageCatalogV4Response updateResponse = cloudbreakClient
                            .getCloudbreakClient()
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
    public void testGetListOfImageCatalogs(TestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName();

        testContext
                .given(imgCatalogName, ImageCatalogTestDto.class)
                .withName(CB_DEFAULT_IMG_CATALOG_NAME)
                .when((testContext1, entity, cloudbreakClient) -> {
                    ImageCatalogV4Responses list = cloudbreakClient
                            .getCloudbreakClient()
                            .imageCatalogV4Endpoint()
                            .list(cloudbreakClient.getWorkspaceId());
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "get prewarmed image if stack info is presented",
            when = "posting a stack with all the repository version",
            then = "getting back a stack which using a prewarmed image")
    public void testGetImageCatalogByNameAndStackWhenPreWarmedImageHasBeenUsed(TestContext testContext) {
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);

        String imgCatalogName = resourcePropertyProvider().getName();
        String environmentName = resourcePropertyProvider().getName();
        String stackName = resourcePropertyProvider().getName();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(mockedTestContext.getImageCatalogMockServerSetup().getPreWarmedImageCatalogUrl())
                .when(imageCatalogTestClient.createV4(), key(imgCatalogName))
                .when(imageCatalogTestClient.setAsDefault(), key(imgCatalogName))
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.createV4(), key(environmentName))
                .given(StackTestDto.class)
                .withCatalog(ImageCatalogTestDto.class)
                .withEnvironment(EnvironmentTestDto.class)
                .withName(stackName)
                .when(stackTestClient.createV4(), key(imgCatalogName))
                .await(STACK_AVAILABLE)
                .given(ImageCatalogTestDto.class)
                .when(imageCatalogTestClient.getImagesByNameV4(stackName), key(imgCatalogName))
                .then((testContext1, entity, cloudbreakClient) -> {
                    ImagesV4Response catalog = entity.getResponseByProvider();
                    if (catalog.getBaseImages().isEmpty() && catalog.getHdpImages().isEmpty() && catalog.getHdfImages().isEmpty()) {
                        String msg = format("The Images response should contain results for MOCK provider and stack with name: '%s'.", stackName);
                        throw new IllegalArgumentException(msg);
                    }
                    return entity;
                })
                .validate();
    }
}
