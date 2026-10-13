package com.sequenceiq.it.cloudbreak.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.mockito.MockedStatic;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.ImageCatalog;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class ImageCatalogDownloaderTest {

    private static final String CATALOG_URL = "https://catalog/v3-dev-freeipa-image-catalog.json";

    private MockedStatic<RestClientUtil> restClientUtil;

    private Response response;

    private final ImageCatalogDownloader underTest = new ImageCatalogDownloader();

    @BeforeMethod
    public void setUp() {
        Client client = mock(Client.class);
        WebTarget webTarget = mock(WebTarget.class);
        Invocation.Builder request = mock(Invocation.Builder.class);
        response = mock(Response.class);
        restClientUtil = mockStatic(RestClientUtil.class);
        restClientUtil.when(RestClientUtil::get).thenReturn(client);
        when(client.target(CATALOG_URL)).thenReturn(webTarget);
        when(webTarget.request()).thenReturn(request);
        when(request.get()).thenReturn(response);
    }

    @AfterMethod
    public void tearDown() {
        restClientUtil.close();
    }

    @Test
    public void parsesSuccessfullyDownloadedCatalog() {
        givenResponse(Response.Status.OK, "{\"images\":{\"freeipa-images\":[]}}");

        ImageCatalog imageCatalog = underTest.download(CATALOG_URL, ImageCatalog.class);

        assertThat(imageCatalog.getImages().getFreeipaImages()).isEmpty();
    }

    @Test
    public void failsWithStatusWhenDownloadIsNotSuccessful() {
        givenResponse(Response.Status.FORBIDDEN, "<Error><Code>AccessDenied</Code></Error>");

        assertThatThrownBy(() -> underTest.download(CATALOG_URL, ImageCatalog.class))
                .isInstanceOf(TestFailException.class)
                .hasMessage("Failed to download image catalog " + CATALOG_URL + ": HTTP 403");
    }

    @Test
    public void failsWithUrlWhenCatalogIsNotJson() {
        givenResponse(Response.Status.OK, "<Error><Code>AccessDenied</Code></Error>");

        assertThatThrownBy(() -> underTest.download(CATALOG_URL, ImageCatalog.class))
                .isInstanceOf(TestFailException.class)
                .hasMessage("Failed to parse image catalog " + CATALOG_URL);
    }

    @Test
    public void failsFastWhenCatalogUrlIsBlank() {
        assertThatThrownBy(() -> underTest.download(" ", ImageCatalog.class))
                .isInstanceOf(TestFailException.class)
                .hasMessage("Image catalog URL is not configured");
    }

    private void givenResponse(Response.Status status, String body) {
        when(response.getStatusInfo()).thenReturn(status);
        when(response.getStatus()).thenReturn(status.getStatusCode());
        when(response.readEntity(String.class)).thenReturn(body);
    }
}
