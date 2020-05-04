package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static java.lang.String.format;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class ImageCatalogPrewarmedTest extends AbstractIntegrationTest {

    private static final String IMG_CATALOG_URL = "https://cloudbreak-imagecatalog.s3.amazonaws.com/v2-prod-cb-image-catalog.json";

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    // TODO: Update endpoint to support CM
    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    @Description(
            given = "get prewarmed image if stack info is presented",
            when = "posting a stack with all the repository version",
            then = "getting back a stack which using a prewarmed image")
    public void testGetImageCatalogByNameAndStackWhenPreWarmedImageHasBeenUsed(TestContext testContext) {
        initializeDefaultBlueprints(testContext);

        String imgCatalogName = resourcePropertyProvider().getName();
        String stackName = resourcePropertyProvider().getName();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(ImageCatalogTestDto.class)
                .withName(imgCatalogName)
                .withUrl(mockedTestContext.getImageCatalogMockServerSetup().getPreWarmedImageCatalogUrl())
                .when(imageCatalogTestClient.createV4(), key(imgCatalogName))
                // .when(imageCatalogTestClient.setAsDefault(), key(imgCatalogName))
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
