package com.sequenceiq.cloudbreak.controller.v4;

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
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.authorization.ImageCatalogFiltering;
import com.sequenceiq.cloudbreak.domain.CustomImage;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.service.image.CustomImageCatalogService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CustomImageCatalogV4ControllerTest {

    private static final Long WORKSPACE_ID = 123L;

    private static final String IMAGE_CATALOG_NAME = "image catalog name";

    private static final String IMAGE_ID = "image id";

    private static final String UUID = java.util.UUID.randomUUID().toString();

    private static final String USER_CRN = "crn:altus:iam:us-west-1:" + UUID + ":user:" + UUID;

    private static final String ACCOUNT_ID = "123";

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private CustomImageCatalogService customImageCatalogService;

    @Mock
    private ConverterUtil converterUtil;

    @InjectMocks
    private CustomImageCatalogV4Controller victim;

    @BeforeAll
    public static void initTest() {
        if (ThreadBasedUserCrnProvider.getUserCrn() == null) {
            ThreadBasedUserCrnProvider.setUserCrn(USER_CRN);
        }
    }

    @Test
    public void testList() {
        Set<ImageCatalog> imageCatalogs = new HashSet<>();
        Set<CustomImageCatalogV4ListItemResponse> customImageCatalogV4ListItemResponses = new HashSet<>();

        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID);
        when(customImageCatalogService.getImageCatalogs(WORKSPACE_ID)).thenReturn(imageCatalogs);
        when(converterUtil.convertAllAsSet(imageCatalogs, CustomImageCatalogV4ListItemResponse.class)).thenReturn(customImageCatalogV4ListItemResponses);

        CustomImageCatalogV4ListResponse actual = victim.list(ACCOUNT_ID);

        assertEquals(customImageCatalogV4ListItemResponses, actual.getResponses());
    }

    @Test
    public void testGet() {
        ImageCatalog imageCatalog = new ImageCatalog();
        CustomImageCatalogV4GetResponse expected = new CustomImageCatalogV4GetResponse();

        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID);
        when(customImageCatalogService.getImageCatalog(WORKSPACE_ID, IMAGE_CATALOG_NAME)).thenReturn(imageCatalog);
        when(converterUtil.convert(imageCatalog, CustomImageCatalogV4GetResponse.class)).thenReturn(expected);

        CustomImageCatalogV4GetResponse actual = victim.get(IMAGE_CATALOG_NAME, ACCOUNT_ID);

        assertEquals(expected, actual);
    }

    @Test
    public void testCreate() {
        CustomImageCatalogV4CreateRequest request = new CustomImageCatalogV4CreateRequest();
        ImageCatalog imageCatalog = new ImageCatalog();
        ImageCatalog savedImageCatalog = new ImageCatalog();
        CustomImageCatalogV4CreateResponse expected = new CustomImageCatalogV4CreateResponse();

        when(converterUtil.convert(request, ImageCatalog.class)).thenReturn(imageCatalog);
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID);
        when(customImageCatalogService.create(eq(imageCatalog), eq(WORKSPACE_ID), anyString(), eq(USER_CRN))).thenReturn(savedImageCatalog);
        when(converterUtil.convert(savedImageCatalog, CustomImageCatalogV4CreateResponse.class)).thenReturn(expected);

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
        when(converterUtil.convert(imageCatalog, CustomImageCatalogV4DeleteResponse.class)).thenReturn(expected);

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
        when(converterUtil.convert(customImage, CustomImageCatalogV4GetImageResponse.class)).thenReturn(expected);
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

        when(converterUtil.convert(request, CustomImage.class)).thenReturn(customImage);
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID);
        when(customImageCatalogService.createCustomImage(eq(WORKSPACE_ID), anyString(), eq(USER_CRN), eq(IMAGE_CATALOG_NAME), eq(customImage)))
                .thenReturn(savedCustomImage);
        when(converterUtil.convert(savedCustomImage, CustomImageCatalogV4CreateImageResponse.class)).thenReturn(expected);

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

        when(converterUtil.convert(request, CustomImage.class)).thenReturn(customImage);
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID);
        when(customImageCatalogService.updateCustomImage(eq(WORKSPACE_ID), eq(USER_CRN), eq(IMAGE_CATALOG_NAME), eq(customImage)))
                .thenReturn(savedCustomImage);
        when(converterUtil.convert(savedCustomImage, CustomImageCatalogV4UpdateImageResponse.class)).thenReturn(expected);

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
        when(converterUtil.convert(customImage, CustomImageCatalogV4DeleteImageResponse.class)).thenReturn(expected);

        CustomImageCatalogV4DeleteImageResponse actual = victim.deleteCustomImage(IMAGE_CATALOG_NAME, IMAGE_ID, ACCOUNT_ID);

        assertEquals(expected, actual);
    }

    private static Image createTestImage() {
        return new Image(null, 12345L, null, null, null, null, null, null, null, null, null, null, null, null, true, null, null);
    }
}