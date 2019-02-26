package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static java.lang.String.format;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.ImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.UpdateImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.it.cloudbreak.newway.Environment;
import com.sequenceiq.it.cloudbreak.newway.EnvironmentEntity;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.action.imagecatalog.ImageCatalogDeleteAction;
import com.sequenceiq.it.cloudbreak.newway.action.imagecatalog.ImageCatalogGetByNameAction;
import com.sequenceiq.it.cloudbreak.newway.action.imagecatalog.ImageCatalogGetImagesByNameAction;
import com.sequenceiq.it.cloudbreak.newway.action.imagecatalog.ImageCatalogGetImagesFromDefaultCatalogAction;
import com.sequenceiq.it.cloudbreak.newway.action.imagecatalog.ImageCatalogPostAction;
import com.sequenceiq.it.cloudbreak.newway.action.imagecatalog.ImageCatalogPostWithoutNameLoggingAction;
import com.sequenceiq.it.cloudbreak.newway.action.imagecatalog.ImageCatalogSetAsDefaultAction;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.util.LongStringGeneratorUtil;

public class ImageCatalogTest extends AbstractIntegrationTest {

    private static final String IMG_CATALOG_URL = "https://cloudbreak-imagecatalog.s3.amazonaws.com/v2-prod-cb-image-catalog.json";

    private static final String IMAGECATALOG_NAME_WITH_SPECIAL_CHARS = "@#$%|:&*;";

    private static final String INVALID_IMAGECATALOG_JSON_URL = "https://rawgit.com/hortonworks/cloudbreak/master/integration-test/src/main/resources/"
            + "templates/imagecatalog_invalid.json";

    private static final String CB_DEFAULT_IMG_CATALOG_NAME = "cloudbreak-default";

    @Inject
    private LongStringGeneratorUtil longStringGeneratorUtil;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((TestContext) data[0]);
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        ((TestContext) data[0]).cleanupTestContextEntity();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testImageCatalogCreationWhenURLIsValidAndExists(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForResource();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(new ImageCatalogPostAction())
                .when(new ImageCatalogGetByNameAction(Boolean.FALSE))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testImageCatalogCreationWhenNameIsTooShort(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForResource().substring(0, 4);

        testContext
                .given(ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(IMG_CATALOG_URL)
                .when(new ImageCatalogPostAction(), RunningParameter.key("ImageCatalogPostAction"))
                .expect(BadRequestException.class, RunningParameter.key("ImageCatalogPostAction")
                        .withExpectedMessage(".*The length of the credential's name has to be in range of 5 to 100"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testImageCatalogCreationWhenNameIsTooLong(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForResource().concat(longStringGeneratorUtil.stringGenerator(100));

        testContext
                .given(ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(IMG_CATALOG_URL)
                .when(new ImageCatalogPostAction(), RunningParameter.key("ImageCatalogPostAction"))
                .expect(BadRequestException.class, RunningParameter.key("ImageCatalogPostAction")
                        .withExpectedMessage(".*The length of the credential's name has to be in range of 5 to 100"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testImageCatalogCreationWhenNameContainsSpecialCharacters(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForResource().concat(IMAGECATALOG_NAME_WITH_SPECIAL_CHARS);

        testContext
                .given(ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(IMG_CATALOG_URL)
                .when(new ImageCatalogPostWithoutNameLoggingAction(), RunningParameter.key("ImageCatalogPostAction"))
                .expect(BadRequestException.class, RunningParameter.key("ImageCatalogPostAction"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testImageCatalogCreationWhenTheCatalogURLIsInvalid(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForResource();
        String invalidURL = "https:/google.com/imagecatalog";

        testContext
                .given(ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(invalidURL)
                .when(new ImageCatalogPostAction(), RunningParameter.key("ImageCatalogPostAction"))
                .expect(BadRequestException.class, RunningParameter.key("ImageCatalogPostAction")
                        .withExpectedMessage(".* " + invalidURL + ", error: A valid image catalog must be available on the given URL"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testImageCatalogCreationWhenTheCatalogURLPointsNotToAnImageCatalogJson(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForResource();

        testContext
                .given(ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(IMG_CATALOG_URL + "/notanimagecatalog")
                .when(new ImageCatalogPostAction(), RunningParameter.key("ImageCatalogPostAction"))
                .expect(BadRequestException.class, RunningParameter.key("ImageCatalogPostAction")
                        .withExpectedMessage(".*A valid image catalog must be available on the given URL.*"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testImageCatalogCreationWhenTheCatalogURLPointsToAnInvalidImageCatalogJson(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForResource();

        testContext
                .given(ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(INVALID_IMAGECATALOG_JSON_URL)
                .when(new ImageCatalogPostAction(), RunningParameter.key("ImageCatalogPostAction"))
                .expect(BadRequestException.class, RunningParameter.key("ImageCatalogPostAction")
                        .withExpectedMessage(".*" + INVALID_IMAGECATALOG_JSON_URL + ", error: A valid image catalog must be available on the given URL.*"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testImageCatalogCreationWhenCatalogWithTheSameNameDeletedRightBeforeCreation(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForResource();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(new ImageCatalogPostAction())
                .select(imgCatalog -> imgCatalog.getResponse().getId(), RunningParameter.key("ImageCatalogPostActionSelect"))
                .when(new ImageCatalogDeleteAction())
                .when(new ImageCatalogPostAction())
                .when(new ImageCatalogGetByNameAction(Boolean.FALSE))
                .then((testContext1, entity, cloudbreakClient) -> {
                    Long firstPostEntityId = testContext1.getSelected("ImageCatalogPostActionSelect");
                    if (entity.getResponse().getId().equals(firstPostEntityId)) {
                        throw new IllegalArgumentException("The re-created ImageCatalog should have a different id.");
                    }
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testImageCatalogDeletion(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForResource();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(new ImageCatalogPostAction())
                .when(new ImageCatalogDeleteAction())
                .when(new ImageCatalogGetByNameAction(), RunningParameter.key("ImageCatalogDeleted"))
                .expect(ForbiddenException.class, RunningParameter.key("ImageCatalogDeleted")
                        .withExpectedMessage("HTTP 403 Forbidden"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testImageCatalogDeletionWithDefaultImageCatalog(TestContext testContext) {
        testContext
                .given(ImageCatalogTestDto.class)
                .withName(CB_DEFAULT_IMG_CATALOG_NAME)
                .when(new ImageCatalogDeleteAction(), RunningParameter.key("ImageCatalogDeleteAction"))
                .expect(BadRequestException.class, RunningParameter.key("ImageCatalogDeleteAction")
                        .withExpectedMessage(format(".*%s cannot be deleted because it is an environment default image catalog.*",
                                CB_DEFAULT_IMG_CATALOG_NAME)))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testGetDefaultImageCatalog(TestContext testContext) {
        testContext
                .given(ImageCatalogTestDto.class).withName(CB_DEFAULT_IMG_CATALOG_NAME)
                .when(new ImageCatalogGetByNameAction(Boolean.FALSE))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testGetImageCatalogWhenCatalogContainsTheRequestedProvider(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForResource();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(new ImageCatalogPostAction())
                .when(new ImageCatalogGetImagesByNameAction())
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
    public void testGetImageCatalogWhenCatalogDoesNotContainTheRequestedProvider(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForResource();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(new ImageCatalogPostAction())
                .when(new ImageCatalogGetImagesByNameAction(CloudPlatform.AWS))
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
    public void testGetImageCatalogWhenTheSpecifiedCatalogDoesNotExistWithName(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForResource();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(new ImageCatalogGetImagesByNameAction(), RunningParameter.key("ImageCatalogDoesNotExist"))
                .expect(ForbiddenException.class, RunningParameter.key("ImageCatalogDoesNotExist"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testGetImageCatalogWhenDefaultCatalogContainsTheRequestedProvider(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForResource();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(new ImageCatalogPostAction())
                .when(new ImageCatalogGetImagesFromDefaultCatalogAction(CloudPlatform.AWS))
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
    public void testGetImageCatalogWhenDefaultCatalogDoesNotContainTheRequestedProvider(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForResource();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(new ImageCatalogPostAction())
                .when(new ImageCatalogGetImagesFromDefaultCatalogAction())
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
    public void testCreateADefaultImageCatalog(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForResource();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(new ImageCatalogPostAction())
                .when(new ImageCatalogSetAsDefaultAction())
                .when(new ImageCatalogGetByNameAction())
                .then((testContext1, entity, cloudbreakClient) -> {
                    ImageCatalogV4Response catalog = entity.getResponse();
                    if (!catalog.isUsedAsDefault()) {
                        throw new IllegalArgumentException("The Images should have been set as default.");
                    }
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testAPreviouslyCreatedDefaultImageCatalogSetAsNotDefault(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForResource();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(new ImageCatalogPostAction())
                .when(new ImageCatalogSetAsDefaultAction())
                .withName(CB_DEFAULT_IMG_CATALOG_NAME)
                .when(new ImageCatalogSetAsDefaultAction())
                .withName(imgCatalogName)
                .when(new ImageCatalogGetByNameAction())
                .then((testContext1, entity, cloudbreakClient) -> {
                    ImageCatalogV4Response catalog = entity.getResponse();
                    if (catalog.isUsedAsDefault()) {
                        throw new IllegalArgumentException(format("The catalog should not have been set as default with name: '%s'", imgCatalogName));
                    }
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testGetImageCatalogsRequestFromExistingImageCatalog(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForResource();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(new ImageCatalogPostAction())
                .select(ImageCatalogTestDto::getRequest, RunningParameter.key("ImageCatalogPostActionSelect"))
                .when((testContext1, entity, cloudbreakClient) -> {
                    ImageCatalogV4Request request = cloudbreakClient
                            .getCloudbreakClient()
                            .imageCatalogV4Endpoint()
                            .getRequest(cloudbreakClient.getWorkspaceId(), imgCatalogName);
                    entity.setRequest(request);
                    return entity;
                })
                .then((testContext1, entity, cloudbreakClient) -> {
                    ImageCatalogV4Request imgCatReq = testContext1.getSelected("ImageCatalogPostActionSelect");
                    if (entity.getRequest().equals(imgCatReq)) {
                        throw new IllegalArgumentException("The requests are not identical.");
                    }
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testUpdateImageCatalog(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForResource();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(new ImageCatalogPostAction())
                .select(ImageCatalogTestDto::getResponse, RunningParameter.key("ImageCatalogPostActionSelect"))
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
                })
                .then((testContext1, entity, cloudbreakClient) -> {
                    ImageCatalogV4Response originalRepsonse = testContext1.getSelected("ImageCatalogPostActionSelect");
                    if (originalRepsonse.getUrl().equals(entity.getResponse().getUrl())) {
                        throw new IllegalArgumentException("The catalog URL should not be the same after update.");
                    }
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testGetListOfImageCatalogs(TestContext testContext) {
        testContext
                .given(ImageCatalogTestDto.class).withName(CB_DEFAULT_IMG_CATALOG_NAME)
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
    public void testGetImageCatalogByNameAndStackWhenPreWarmedImageHasBeenUsed(TestContext testContext) {
        createDefaultCredential(testContext);
        initializeDefaultClusterDefinitions(testContext);

        String imgCatalogName = getNameGenerator().getRandomNameForResource();
        String stackName = imgCatalogName + "-stack";
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(mockedTestContext.getImageCatalogMockServerSetup().getPreWarmedImageCatalogUrl())
                .when(new ImageCatalogPostAction())
                .when(new ImageCatalogSetAsDefaultAction())

                .given(EnvironmentEntity.class)
                .when(Environment::post)

                .given(StackTestDto.class)
                .withEnvironment(EnvironmentEntity.class)
                .withName(stackName)
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)

                .given(ImageCatalogTestDto.class)
                .when(new ImageCatalogGetImagesByNameAction(stackName))
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

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    public void testGetImageCatalogByNameAndStackWhenBaseImageHasBeenUsed(TestContext testContext) {
        createDefaultCredential(testContext);
        initializeDefaultClusterDefinitions(testContext);

        String imgCatalogName = getNameGenerator().getRandomNameForResource();
        String stackName = imgCatalogName + "-stack";
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(new ImageCatalogPostAction())
                .when(new ImageCatalogSetAsDefaultAction())

                .given(EnvironmentEntity.class)
                .when(Environment::post)

                .given(StackTestDto.class)
                .withEnvironment(EnvironmentEntity.class)
                .withName(stackName)
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)

                .given(ImageCatalogTestDto.class)
                .when(new ImageCatalogGetImagesByNameAction(stackName))
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
