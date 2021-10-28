package com.sequenceiq.cloudbreak.controller.v4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.request.CustomImageCatalogV4CreateImageRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.request.CustomImageCatalogV4CreateRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.request.CustomImageCatalogV4UpdateImageRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4CreateImageResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4CreateResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4DeleteImageResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4DeleteResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4GetImageResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4GetResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4ListItemResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4ListResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4UpdateImageResponse;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.authorization.ImageCatalogFiltering;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.converter.v4.customimage.CustomImageCatalogV4CreateImageRequestToCustomImageConverter;
import com.sequenceiq.cloudbreak.converter.v4.customimage.CustomImageCatalogV4CreateRequestToImageCatalogConverter;
import com.sequenceiq.cloudbreak.converter.v4.customimage.CustomImageCatalogV4UpdateImageRequestToCustomImageConverter;
import com.sequenceiq.cloudbreak.converter.v4.customimage.CustomImageToCustomImageCatalogV4CreateImageResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.customimage.CustomImageToCustomImageCatalogV4DeleteImageResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.customimage.CustomImageToCustomImageCatalogV4GetImageResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.customimage.CustomImageToCustomImageCatalogV4UpdateImageResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.customimage.ImageCatalogToCustomImageCatalogV4CreateResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.customimage.ImageCatalogToCustomImageCatalogV4DeleteResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.customimage.ImageCatalogToCustomImageCatalogV4GetResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.customimage.ImageCatalogToCustomImageCatalogV4ListItemResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.imagecatalog.ImageCatalogV4RequestToImageCatalogConverter;
import com.sequenceiq.cloudbreak.domain.CustomImage;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.service.image.CustomImageCatalogService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;

@ExtendWith(MockitoExtension.class)
public class CustomImageCatalogV4ControllerTest {

    private static final Long WORKSPACE_ID = 123L;

    private static final String IMAGE_CATALOG_NAME = "image catalog name";

    private static final String IMAGE_ID = "image id";

    private static final String USER_CRN = "crn:altus:iam:us-west-1:1:user:2";

    private static final String ACCOUNT_ID = "123";

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private CustomImageCatalogService customImageCatalogService;

    @Mock
    private ImageCatalogFiltering imageCatalogFiltering;

    @Mock
    private ImageCatalogToCustomImageCatalogV4ListItemResponseConverter imageCatalogToCustomImageCatalogV4ListItemResponseConverter;

    @Mock
    private CustomImageToCustomImageCatalogV4GetImageResponseConverter customImageToCustomImageCatalogV4GetImageResponseConverter;

    @Mock
    private ImageCatalogV4RequestToImageCatalogConverter imageCatalogV4RequestToImageCatalogConverter;

    @Mock
    private CustomImageToCustomImageCatalogV4CreateImageResponseConverter customImageToCustomImageCatalogV4CreateImageResponseConverter;

    @Mock
    private CustomImageToCustomImageCatalogV4DeleteImageResponseConverter customImageToCustomImageCatalogV4DeleteImageResponseConverter;

    @Mock
    private CustomImageCatalogV4CreateImageRequestToCustomImageConverter customImageCatalogV4CreateImageRequestToCustomImageConverter;

    @Mock
    private CustomImageToCustomImageCatalogV4UpdateImageResponseConverter customImageToCustomImageCatalogV4UpdateImageResponseConverter;

    @Mock
    private CustomImageCatalogV4UpdateImageRequestToCustomImageConverter customImageCatalogV4UpdateImageRequestToCustomImageConverter;

    @Mock
    private CustomImageCatalogV4CreateRequestToImageCatalogConverter customImageCatalogV4CreateRequestToImageCatalogConverter;

    @Mock
    private ImageCatalogToCustomImageCatalogV4GetResponseConverter imageCatalogToCustomImageCatalogV4GetResponseConverter;

    @Mock
    private ImageCatalogToCustomImageCatalogV4CreateResponseConverter imageCatalogToCustomImageCatalogV4CreateResponseConverter;

    @Mock
    private ImageCatalogToCustomImageCatalogV4DeleteResponseConverter imageCatalogToCustomImageCatalogV4DeleteResponseConverter;

    @InjectMocks
    private CustomImageCatalogV4Controller victim;

    @Test
    public void testList() {
        Set<ImageCatalog> imageCatalogs = new HashSet<>();
        Set<CustomImageCatalogV4ListItemResponse> customImageCatalogV4ListItemResponses = new HashSet<>();

        when(imageCatalogFiltering.filterImageCatalogs(eq(AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG), eq(true))).thenReturn(imageCatalogs);

        CustomImageCatalogV4ListResponse actual = victim.list(ACCOUNT_ID);

        assertEquals(customImageCatalogV4ListItemResponses, actual.getResponses());
    }

    @Test
    public void testGet() {
        ImageCatalog imageCatalog = new ImageCatalog();
        CustomImageCatalogV4GetResponse expected = new CustomImageCatalogV4GetResponse();

        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID);
        when(customImageCatalogService.getImageCatalog(WORKSPACE_ID, IMAGE_CATALOG_NAME)).thenReturn(imageCatalog);
        when(imageCatalogToCustomImageCatalogV4GetResponseConverter.convert(imageCatalog)).thenReturn(expected);

        CustomImageCatalogV4GetResponse actual = victim.get(IMAGE_CATALOG_NAME, ACCOUNT_ID);

        assertEquals(expected, actual);
    }

    @Test
    public void testCreate() {
        CustomImageCatalogV4CreateRequest request = new CustomImageCatalogV4CreateRequest();
        ImageCatalog imageCatalog = new ImageCatalog();
        ImageCatalog savedImageCatalog = new ImageCatalog();
        CustomImageCatalogV4CreateResponse expected = new CustomImageCatalogV4CreateResponse();

        when(customImageCatalogV4CreateRequestToImageCatalogConverter.convert(request)).thenReturn(imageCatalog);
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID);
        when(customImageCatalogService.create(eq(imageCatalog), eq(WORKSPACE_ID), anyString(), eq(USER_CRN))).thenReturn(savedImageCatalog);
        when(imageCatalogToCustomImageCatalogV4CreateResponseConverter.convert(savedImageCatalog)).thenReturn(expected);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            CustomImageCatalogV4CreateResponse actual = victim.create(request, ACCOUNT_ID);

            assertEquals(expected, actual);
        });
    }

    @Test
    public void testDelete() {
        CustomImageCatalogV4DeleteResponse expected = new CustomImageCatalogV4DeleteResponse();
        ImageCatalog imageCatalog = new ImageCatalog();

        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID);
        when(customImageCatalogService.delete(WORKSPACE_ID, IMAGE_CATALOG_NAME)).thenReturn(imageCatalog);
        when(imageCatalogToCustomImageCatalogV4DeleteResponseConverter.convert(imageCatalog)).thenReturn(expected);

        CustomImageCatalogV4DeleteResponse actual = victim.delete(IMAGE_CATALOG_NAME, ACCOUNT_ID);

        assertEquals(expected, actual);
    }

    @Test
    public void testGetCustomImage() {
        CustomImage customImage = new CustomImage();
        Image sourceImage = createTestImage();

        CustomImageCatalogV4GetImageResponse expected = new CustomImageCatalogV4GetImageResponse();
        expected.setSourceImageDate(12345L);

        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID);
        when(customImageCatalogService.getCustomImage(WORKSPACE_ID, IMAGE_CATALOG_NAME, IMAGE_ID)).thenReturn(customImage);
        when(customImageToCustomImageCatalogV4GetImageResponseConverter.convert(customImage)).thenReturn(expected);
        when(customImageCatalogService.getSourceImage(customImage)).thenReturn(sourceImage);

        CustomImageCatalogV4GetImageResponse actual = victim.getCustomImage(IMAGE_CATALOG_NAME, IMAGE_ID, ACCOUNT_ID);

        assertEquals(expected, actual);
    }

    @Test
    public void testCreateCustomImage() {
        CustomImageCatalogV4CreateImageRequest request = new CustomImageCatalogV4CreateImageRequest();
        CustomImage customImage = new CustomImage();
        CustomImage savedCustomImage = new CustomImage();
        CustomImageCatalogV4CreateImageResponse expected = new CustomImageCatalogV4CreateImageResponse();

        when(customImageCatalogV4CreateImageRequestToCustomImageConverter.convert(request)).thenReturn(customImage);
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID);
        when(customImageCatalogService.createCustomImage(eq(WORKSPACE_ID), anyString(), eq(USER_CRN), eq(IMAGE_CATALOG_NAME), eq(customImage)))
                .thenReturn(savedCustomImage);
        when(customImageToCustomImageCatalogV4CreateImageResponseConverter.convert(savedCustomImage)).thenReturn(expected);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            CustomImageCatalogV4CreateImageResponse actual = victim.createCustomImage(IMAGE_CATALOG_NAME, request, ACCOUNT_ID);

            assertEquals(expected, actual);
        });
    }

    @Test
    public void testUpdateCustomImage() {
        CustomImageCatalogV4UpdateImageRequest request = new CustomImageCatalogV4UpdateImageRequest();
        CustomImage customImage = new CustomImage();
        CustomImage savedCustomImage = new CustomImage();
        CustomImageCatalogV4UpdateImageResponse expected = new CustomImageCatalogV4UpdateImageResponse();

        when(customImageCatalogV4UpdateImageRequestToCustomImageConverter.convert(request)).thenReturn(customImage);
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID);
        when(customImageCatalogService.updateCustomImage(eq(WORKSPACE_ID), eq(USER_CRN), eq(IMAGE_CATALOG_NAME), eq(customImage)))
                .thenReturn(savedCustomImage);
        when(customImageToCustomImageCatalogV4UpdateImageResponseConverter.convert(savedCustomImage)).thenReturn(expected);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            CustomImageCatalogV4UpdateImageResponse actual = victim.updateCustomImage(IMAGE_CATALOG_NAME, IMAGE_ID, request, ACCOUNT_ID);

            assertEquals(expected, actual);
        });
    }

    @Test
    public void testDeleteCustomImage() {
        CustomImage customImage = new CustomImage();
        CustomImageCatalogV4DeleteImageResponse expected = new CustomImageCatalogV4DeleteImageResponse();

        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID);
        when(customImageCatalogService.deleteCustomImage(WORKSPACE_ID, IMAGE_CATALOG_NAME, IMAGE_ID)).thenReturn(customImage);
        when(customImageToCustomImageCatalogV4DeleteImageResponseConverter.convert(customImage)).thenReturn(expected);

        CustomImageCatalogV4DeleteImageResponse actual = victim.deleteCustomImage(IMAGE_CATALOG_NAME, IMAGE_ID, ACCOUNT_ID);

        assertEquals(expected, actual);
    }

    private static Image createTestImage() {
        return new Image(null, 12345L, null, null, null, null, null, null, null, null, null, null, null, null, true, null, null);
    }
}