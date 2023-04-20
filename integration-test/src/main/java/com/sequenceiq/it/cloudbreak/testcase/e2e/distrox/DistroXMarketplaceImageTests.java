package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static java.lang.String.format;

import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CloudProvider;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;

public class DistroXMarketplaceImageTests extends AbstractE2ETest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXMarketplaceImageTests.class);

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        initalizeAzureMarketplaceTermsPolicy(testContext);
        createDefaultDatalake(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid DistroX create request is sent (latest Marketplace Image)",
            then = "DistroX should be available and deletable")
    public void testDistroXWithMarketplaceImageCanBeCreatedSuccessfully(TestContext testContext) {
        String imageSettings = resourcePropertyProvider().getName();
        String distrox = resourcePropertyProvider().getName();
        AtomicReference<String> selectedImageID = new AtomicReference<>();
        CloudProvider cloudProvider = testContext.getCloudProvider();

        String imgCatalogKey = "mp-img-cat";
        testContext
                .given(imgCatalogKey, ImageCatalogTestDto.class)
                .withName(imgCatalogKey)
                .withUrl("https://cloudbreak-imagecatalog.s3.amazonaws.com/v3-marketplace-image-catalog.json")
                .when(imageCatalogTestClient.createIfNotExistV4())
                .when((tc, dto, client) -> {
                    selectedImageID.set(cloudProvider.getLatestPreWarmedImageIDByRuntime(tc, dto, client, "7.2.16"));
                    return dto;
                })
                .given(imageSettings, DistroXImageTestDto.class)
                    .withImageCatalog(imgCatalogKey)
                    .withImageId(selectedImageID.get())
                .given(distrox, DistroXTestDto.class)
                    .withImageSettings(imageSettings)
                .when(distroXTestClient.create(), key(distrox))
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .then((tc, dto, client) -> {
                    Log.log(LOGGER, format(" Image Catalog Name: %s ", dto.getResponse().getImage().getCatalogName()));
                    Log.log(LOGGER, format(" Image Catalog URL: %s ", dto.getResponse().getImage().getCatalogUrl()));
                    Log.log(LOGGER, format(" Image ID: %s ", dto.getResponse().getImage().getId()));

                    if (!dto.getResponse().getImage().getId().equals(selectedImageID.get())) {
                        throw new TestFailException(" The selected image ID is: " + dto.getResponse().getImage().getId() + " instead of: "
                                + selectedImageID.get());
                    }
                    return dto;
                })
                .validate();
    }
}
