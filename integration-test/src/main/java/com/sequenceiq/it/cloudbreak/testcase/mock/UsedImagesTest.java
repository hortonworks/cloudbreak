package com.sequenceiq.it.cloudbreak.testcase.mock;

import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.UsedImageStacksV4Response;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.util.model.UsedImageStacksV1Response;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.client.UtilTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeipaUsedImagesTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.UsedImagesTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class UsedImagesTest extends AbstractMockTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsedImagesTest.class);

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private UtilTestClient utilTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    private String freeipaImageUuid;

    private String sdxImageUuid;

    @Override
    protected void setupTest(TestContext testContext) {
        freeipaImageUuid = UUID.randomUUID().toString();
        sdxImageUuid = UUID.randomUUID().toString();

        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a running environment with an sdx",
            when = "list used images requests are sent to cb and freeipa",
            then = "response should have the images",
            and = "after deleting env used images should not those images"
    )
    public void testUsedImages(MockedTestContext testContext) {
        String cbImageCatalogName = resourcePropertyProvider().getName();

        testContext
                .given(ImageCatalogTestDto.class)
                    .withName(cbImageCatalogName)
                    .withUrl(getImageCatalogMockServerSetup().getPreWarmedImageCatalogUrlWithDefaultImageUuid(sdxImageUuid))
                .when(imageCatalogTestClient.createV4())

                .given(EnvironmentNetworkTestDto.class)
                .given(EnvironmentTestDto.class)
                    .withNetwork()
                    .withCreateFreeIpa(Boolean.FALSE)
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .when(environmentTestClient.describe())

                .given(FreeIpaTestDto.class)
                    .withCatalog(getImageCatalogMockServerSetup().getFreeIpaImageCatalogUrlWitdDefaultImageUuid(freeipaImageUuid))
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .when(freeIpaTestClient.describe())

                .given(SdxInternalTestDto.class)
                    .withImageCatalogNameAndImageId(cbImageCatalogName, sdxImageUuid)
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .when(sdxTestClient.describeInternal())

                .then((tc, testDto, client) -> verifyImagesAreUsed(tc, testDto))

                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.delete())
                .await(EnvironmentStatus.ARCHIVED)

                .then((tc, testDto, client) -> verifyImagesAreNotUsed(tc, testDto))
                .validate();
    }

    private <T extends CloudbreakTestDto> T verifyImagesAreUsed(TestContext testContext, T testDto) {
        testContext
                .given(UsedImagesTestDto.class)
                .when(utilTestClient.usedImages())
                .then((tc, usedImagesTestDto, client) -> {
                    List<UsedImageStacksV4Response> usedImages = usedImagesTestDto.getResponse().getUsedImages();
                    UsedImageStacksV4Response usedImageStacksV4Response = usedImages.stream()
                            .filter(usedImage -> usedImage.getImage().getImageId().contains(sdxImageUuid))
                            .findFirst().orElseThrow(() -> new TestFailException(String.format("SDX image is NOT in use with ID:: %s", sdxImageUuid)));
                    LOGGER.info("Used SDX image ID:: {}", usedImageStacksV4Response.getImage().getImageId());
                    return usedImagesTestDto;
                })

                .given(FreeipaUsedImagesTestDto.class)
                .when(freeIpaTestClient.usedImages())
                .then((tc, usedImagesTestDto, client) -> {
                    List<UsedImageStacksV1Response> usedImages = usedImagesTestDto.getResponse().getUsedImages();
                    UsedImageStacksV1Response usedImageStacksV1Response = usedImages.stream()
                            .filter(usedImage -> usedImage.getImage().getImageId().contains(freeipaImageUuid))
                            .findFirst().orElseThrow(() -> new TestFailException(String.format("FreeIpa image is NOT in use with ID:: %s",
                                    freeipaImageUuid)));
                    LOGGER.info("Used FreeIpa image ID:: {}", usedImageStacksV1Response.getImage().getImageId());
                    return usedImagesTestDto;
                })
                .validate();
        return testDto;
    }

    private <T extends CloudbreakTestDto> T verifyImagesAreNotUsed(TestContext testContext, T testDto) {
        testContext
                .given(UsedImagesTestDto.class)
                .when(utilTestClient.usedImages())
                .then((tc, usedImagesTestDto, client) -> {
                    List<UsedImageStacksV4Response> usedImages = usedImagesTestDto.getResponse().getUsedImages();
                    if (usedImages.stream()
                            .noneMatch(usedImage -> usedImage.getImage().getImageId().contains(sdxImageUuid))) {
                        LOGGER.info("SDX image (ID:: {}) is not in use anymore", sdxImageUuid);
                    } else {
                        throw new TestFailException(String.format("SDX image (ID:: %s) is still in use!", sdxImageUuid));
                    }
                    return usedImagesTestDto;
                })

                .given(FreeipaUsedImagesTestDto.class)
                .when(freeIpaTestClient.usedImages())
                .then((tc, usedImagesTestDto, client) -> {
                    List<UsedImageStacksV1Response> usedImages = usedImagesTestDto.getResponse().getUsedImages();
                    if (usedImages.stream()
                            .noneMatch(usedImage -> usedImage.getImage().getImageId().contains(freeipaImageUuid))) {
                        LOGGER.info("FreeIpa image (ID:: {}) is not in use anymore", freeipaImageUuid);
                    } else {
                        throw new TestFailException(String.format("FreeIpa image (ID:: %s) is still in use!", freeipaImageUuid));
                    }
                    return usedImagesTestDto;
                })
                .validate();
        return testDto;
    }
}
