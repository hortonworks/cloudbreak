package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.dto.mock.CheckCount.atLeast;
import static com.sequenceiq.it.cloudbreak.mock.ImageCatalogMockServerSetup.responseFromJsonFile;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.action.v4.credential.CredentialCreateAction;
import com.sequenceiq.it.cloudbreak.action.v4.imagecatalog.ImageCatalogCreateRetryAction;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.mock.HttpMock;
import com.sequenceiq.it.cloudbreak.dto.mock.endpoint.ImageCatalogEndpoint;
import com.sequenceiq.it.cloudbreak.mock.ImageCatalogMockServerSetup;
import com.sequenceiq.it.cloudbreak.testcase.AbstractMinimalTest;

public class ImageCatalogBasicTest extends AbstractMinimalTest {

    public static final String RETURN_WITH_EMPTY = "";

    @Inject
    ImageCatalogMockServerSetup imageCatalogMockServerSetup;

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    @Inject
    private TestParameter testParameter;

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "image catalog valid URL",
            when = "calling create image catalog with that URL",
            then = "getting image catalog response so the creation success")
    public void testIC(TestContext testContext) {
        String imgCatalogName = resourcePropertyProvider().getName();

        testContext
                .as()
                .given(HttpMock.class)
                .whenRequested(ImageCatalogEndpoint.Base.class).getCatalog().thenReturn(
                (model, uriParameters) -> imageCatalogMockServerSetup
                        .patchCbVersion(responseFromJsonFile("imagecatalog/catalog.json"), testParameter))
                .whenRequested(ImageCatalogEndpoint.Base.class).head()
                .thenReturnHeader("Content-Length", "38")
                .thenReturn((model, uriParameters) -> RETURN_WITH_EMPTY)
                .given(ImageCatalogTestDto.class)
                .withUrl(httpMock -> httpMock.whenRequested(ImageCatalogEndpoint.Base.class).getCatalog().getFullUrl())
                .withName(imgCatalogName)
                .when(new ImageCatalogCreateRetryAction())
                .when(imageCatalogTestClient.getV4(Boolean.FALSE))
                .given(HttpMock.class)
                .then(ImageCatalogEndpoint.Base.class).head().verify(atLeast(1))
                .then(ImageCatalogEndpoint.Base.class).getCatalog().verify(atLeast(1))
                .given(CredentialTestDto.class)
                .when(new CredentialCreateAction())
                .validate();
    }
}
