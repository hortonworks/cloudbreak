package com.sequenceiq.datalake.service.imagecatalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.ImageCatalogV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.BaseImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.BaseStackDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceCrnEndpoints;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.ImageCatalogPlatform;
import com.sequenceiq.datalake.service.sdx.SdxService;

@ExtendWith(MockitoExtension.class)
public class ImageCatalogServiceTest {

    private static final String IMAGE_CATALOG_NAME = "image catalog name";

    private static final String IMAGE_CATALOG_CRN = "image catalog crn";

    private static final String IMAGE_ID = "image id";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:hortonworks:user:perdos@hortonworks.com";

    private static final ImageCatalogPlatform IMAGE_CATALOG_PLATFORM_AWS = ImageCatalogPlatform.imageCatalogPlatform(CloudPlatform.AWS.toString());

    private static final ImageCatalogPlatform IMAGE_CATALOG_PLATFORM_AZURE = ImageCatalogPlatform.imageCatalogPlatform(CloudPlatform.AZURE.toString());

    @Mock
    private CloudbreakInternalCrnClient cloudbreakInternalCrnClient;

    @Mock
    private CloudbreakServiceCrnEndpoints cloudbreakServiceCrnEndpoints;

    @Mock
    private ImageCatalogV4Endpoint imageCatalogV4Endpoint;

    @Mock
    private ImageCatalogV4Response imageCatalogV4Response;

    @InjectMocks
    private ImageCatalogService victim;

    @BeforeEach
    void setUp() {
        lenient().when(cloudbreakInternalCrnClient.withInternalCrn()).thenReturn(cloudbreakServiceCrnEndpoints);
        lenient().when(cloudbreakServiceCrnEndpoints.imageCatalogV4Endpoint()).thenReturn(imageCatalogV4Endpoint);
    }

    @Test
    public void supportedAuthorizationResourceTypeShouldBeImageCatalog() {
        AuthorizationResourceType actual = victim.getSupportedAuthorizationResourceType();

        assertEquals(AuthorizationResourceType.IMAGE_CATALOG, actual);
    }

    @Test
    public void resourceCrnByResourceNameShouldBeResolvedByCore() {
        when(cloudbreakInternalCrnClient.withInternalCrn()).thenReturn(cloudbreakServiceCrnEndpoints);
        when(cloudbreakServiceCrnEndpoints.imageCatalogV4Endpoint()).thenReturn(imageCatalogV4Endpoint);
        when(imageCatalogV4Endpoint.getByNameInternal(eq(SdxService.WORKSPACE_ID_DEFAULT), eq(IMAGE_CATALOG_NAME), eq(false), any()))
                .thenReturn(imageCatalogV4Response);
        when(imageCatalogV4Response.getCrn()).thenReturn(IMAGE_CATALOG_CRN);

        String actual = victim.getResourceCrnByResourceName(IMAGE_CATALOG_NAME);
        assertEquals(IMAGE_CATALOG_CRN, actual);
    }

    @Test
    public void testGetImageResponseFromImageRequestWithPrewarmImage() throws Exception {
        ImageV4Response imageResponse = getCdhImageResponse();
        ImagesV4Response imagesV4Response = new ImagesV4Response();
        imagesV4Response.setCdhImages(List.of(imageResponse));

        ImageSettingsV4Request imageSettingsV4Request = new ImageSettingsV4Request();
        imageSettingsV4Request.setCatalog(IMAGE_CATALOG_NAME);
        imageSettingsV4Request.setId(IMAGE_ID);

        when(imageCatalogV4Endpoint.getImageByCatalogNameAndImageId(any(), eq(IMAGE_CATALOG_NAME), eq(IMAGE_ID), any())).thenReturn(imagesV4Response);

        ImageV4Response actual = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> victim.getImageResponseFromImageRequest(imageSettingsV4Request, IMAGE_CATALOG_PLATFORM_AWS));
        assertEquals(imageResponse, actual);
    }

    @Test
    public void testGetImageResponseFromImageRequestWithBaseImage() throws Exception {
        BaseImageV4Response imageResponse = getBaseImageResponse();
        ImagesV4Response imagesV4Response = new ImagesV4Response();
        imagesV4Response.setBaseImages(List.of(imageResponse));

        ImageSettingsV4Request imageSettingsV4Request = new ImageSettingsV4Request();
        imageSettingsV4Request.setCatalog(IMAGE_CATALOG_NAME);
        imageSettingsV4Request.setId(IMAGE_ID);

        when(imageCatalogV4Endpoint.getImageByCatalogNameAndImageId(any(), eq(IMAGE_CATALOG_NAME), eq(IMAGE_ID), any())).thenReturn(imagesV4Response);

        ImageV4Response actual = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> victim.getImageResponseFromImageRequest(imageSettingsV4Request, IMAGE_CATALOG_PLATFORM_AWS));
        assertEquals(imageResponse, actual);
    }

    @Test
    void testGetImageResponseFromImageRequestWithoutImageSettingsRequest() {
        ImageV4Response actual = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> victim.getImageResponseFromImageRequest(null, IMAGE_CATALOG_PLATFORM_AWS));
        assertNull(actual);
    }

    @Test
    void testGetImageResponseFromImageRequestWithImageSettingsRequestNotResultingInImages() throws Exception {
        ImageSettingsV4Request imageSettingsV4Request = new ImageSettingsV4Request();
        imageSettingsV4Request.setCatalog(IMAGE_CATALOG_NAME);
        imageSettingsV4Request.setId(IMAGE_ID);

        when(imageCatalogV4Endpoint.getImageByCatalogNameAndImageId(any(), eq(IMAGE_CATALOG_NAME), eq(IMAGE_ID), any())).thenReturn(null);

        ImageV4Response actual = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> victim.getImageResponseFromImageRequest(imageSettingsV4Request, IMAGE_CATALOG_PLATFORM_AWS));
        assertNull(actual);
    }

    @Test
    void testGetImageResponseFromImageRequestWhenEndpointFails() throws Exception {
        doThrow(new Exception()).when(imageCatalogV4Endpoint).getImageByImageId(any(), any(), any());

        ImageV4Response actual = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> victim.getImageResponseFromImageRequest(new ImageSettingsV4Request(), IMAGE_CATALOG_PLATFORM_AWS));
        assertNull(actual);
        verify(imageCatalogV4Endpoint).getImageByImageId(any(), any(), any());
    }

    @Test
    void testGetImageResponseFromImageRequestWithImageSettingsRequestResultingInImagesWithDifferentProvider() throws Exception {
        ImageV4Response imageResponse = getCdhImageResponse();
        ImagesV4Response imagesV4Response = new ImagesV4Response();
        imagesV4Response.setCdhImages(List.of(imageResponse));
        imagesV4Response.setBaseImages(List.of(getBaseImageResponse()));

        ImageSettingsV4Request imageSettingsV4Request = new ImageSettingsV4Request();
        imageSettingsV4Request.setCatalog(IMAGE_CATALOG_NAME);
        imageSettingsV4Request.setId(IMAGE_ID);

        when(imageCatalogV4Endpoint.getImageByCatalogNameAndImageId(any(), eq(IMAGE_CATALOG_NAME), eq(IMAGE_ID), any())).thenReturn(imagesV4Response);

        ImageV4Response actual = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> victim.getImageResponseFromImageRequest(imageSettingsV4Request, IMAGE_CATALOG_PLATFORM_AZURE));
        assertNull(actual);
    }

    private ImageV4Response getCdhImageResponse() {
        Map<String, Map<String, String>> imageSetsByProvider = new HashMap<>();
        imageSetsByProvider.put("aws", null);
        BaseStackDetailsV4Response stackDetails = new BaseStackDetailsV4Response();
        stackDetails.setVersion("7.2.7");

        ImageV4Response imageV4Response = new ImageV4Response();
        imageV4Response.setImageSetsByProvider(imageSetsByProvider);
        imageV4Response.setStackDetails(stackDetails);
        return imageV4Response;
    }

    private BaseImageV4Response getBaseImageResponse() {
        Map<String, Map<String, String>> imageSetsByProvider = new HashMap<>();
        imageSetsByProvider.put("aws", null);

        BaseImageV4Response imageV4Response = new BaseImageV4Response();
        imageV4Response.setImageSetsByProvider(imageSetsByProvider);
        return imageV4Response;
    }
}
