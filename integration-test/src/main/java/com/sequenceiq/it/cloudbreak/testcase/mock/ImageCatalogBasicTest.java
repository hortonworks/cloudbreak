package com.sequenceiq.it.cloudbreak.testcase.mock;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.action.v4.imagecatalog.ImageCatalogCreateRetryAction;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractMinimalTest;

public class ImageCatalogBasicTest extends AbstractMinimalTest {

    public static final String RETURN_WITH_EMPTY = "";

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    @Inject
    private TestParameter testParameter;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "image catalog valid URL",
            when = "calling create image catalog with that URL",
            then = "getting image catalog response so the creation success")
    public void testIC(MockedTestContext testContext) {
        createImageCatalogWithUrl(testContext, testContext.getImageCatalogMockServerSetup().getImageCatalogUrl());
        createImageCatalogWithUrl(testContext, testContext.getImageCatalogMockServerSetup().getPreWarmedImageCatalogUrl());
        createImageCatalogWithUrl(testContext, testContext.getImageCatalogMockServerSetup().getUpgradeImageCatalogUrl());
    }

    public void createImageCatalogWithUrl(MockedTestContext testContext, String url) {
        String imgCatalogName = resourcePropertyProvider().getName();
        testContext
                .as()
                .given(ImageCatalogTestDto.class)
                .withUrl(url)
                .withName(imgCatalogName)
                .when(new ImageCatalogCreateRetryAction())
                .when(imageCatalogTestClient.getV4())
                .validate();
    }
}
