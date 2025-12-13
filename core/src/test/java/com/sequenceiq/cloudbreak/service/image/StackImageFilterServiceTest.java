package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.common.model.ImageCatalogPlatform.imageCatalogPlatform;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.model.ImageCatalogPlatform;

@ExtendWith(MockitoExtension.class)
class StackImageFilterServiceTest {

    private static final String CUSTOM_IMAGE_CATALOG_URL = "http://localhost/custom-imagecatalog-url";

    private static final String PROVIDER_AWS = "AWS";

    public static final ImageCatalogPlatform AWS = imageCatalogPlatform(PROVIDER_AWS);

    private static final String STACK_NAME = "stackName";

    private static final String IMAGE_CATALOG_NAME = "anyImageCatalog";

    private static final String IMAGE_HDP_ID = "hdp-1";

    private static final String IMAGE_BASE_ID = "base-2";

    private static final String IMAGE_HDF_ID = "hdf-3";

    private static final String IMAGE_CDH_ID = "cdh-4";

    private static final long STACK_ID = 1L;

    private static final long ORG_ID = 100L;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private StackService stackService;

    @Mock
    private StackImageUpdateService stackImageUpdateService;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private PlatformStringTransformer platformStringTransformer;

    @InjectMocks
    private StackImageFilterService underTest;

    @Test
    void testGetApplicableImagesCdh() throws CloudbreakImageCatalogException, CloudbreakImageNotFoundException {
        Stack stack = getStack(DetailedStackStatus.AVAILABLE);
        setupLoggedInUser();
        when(imageCatalogService.getImages(anyLong(), anyString(), any(), any(), anyBoolean(), eq(null))).thenReturn(getStatedImages());
        when(stackService.getByNameInWorkspaceWithLists(eq(STACK_NAME), eq(ORG_ID))).thenReturn(Optional.ofNullable(stack));
        when(stackImageUpdateService.isValidImage(any(), anyString(), anyString(), anyString())).thenReturn(true);
        when(componentConfigProviderService.getImage(anyLong())).thenReturn(getImage(""));
        when(platformStringTransformer.getPlatformStringForImageCatalog(anyString(), anyString())).thenReturn(AWS);

        Images images = underTest.getApplicableImages(ORG_ID, IMAGE_CATALOG_NAME, STACK_NAME, false);

        assertEquals(IMAGE_BASE_ID, images.getBaseImages().get(0).getUuid());
        assertEquals(IMAGE_CDH_ID, images.getCdhImages().get(0).getUuid());
        verify(imageCatalogService).getImages(eq(ORG_ID), eq(IMAGE_CATALOG_NAME), isNull(), eq(AWS), eq(false), eq(null));
        verify(componentConfigProviderService).getImage(STACK_ID);
        verify(stackImageUpdateService).isValidImage(eq(stack), eq(IMAGE_BASE_ID), eq(IMAGE_CATALOG_NAME), eq(CUSTOM_IMAGE_CATALOG_URL));
        verify(stackImageUpdateService, never()).isValidImage(eq(stack), eq(IMAGE_HDP_ID), eq(IMAGE_CATALOG_NAME), eq(CUSTOM_IMAGE_CATALOG_URL));
        verify(stackImageUpdateService).isValidImage(eq(stack), eq(IMAGE_CDH_ID), eq(IMAGE_CATALOG_NAME), eq(CUSTOM_IMAGE_CATALOG_URL));
    }

    @Test
    void testGetApplicableImagesFiltersCurrentImage() throws CloudbreakImageCatalogException, CloudbreakImageNotFoundException {
        Stack stack = getStack(DetailedStackStatus.AVAILABLE);
        setupLoggedInUser();
        when(imageCatalogService.getImages(anyLong(), anyString(), any(), any(), anyBoolean(), eq(null))).thenReturn(getStatedImages());
        when(stackService.getByNameInWorkspaceWithLists(eq(STACK_NAME), eq(ORG_ID))).thenReturn(Optional.ofNullable(stack));
        when(stackImageUpdateService.isValidImage(any(), anyString(), anyString(), anyString())).thenReturn(true);
        when(componentConfigProviderService.getImage(anyLong())).thenReturn(getImage(IMAGE_HDP_ID));
        when(platformStringTransformer.getPlatformStringForImageCatalog(anyString(), anyString())).thenReturn(AWS);

        Images images = underTest.getApplicableImages(ORG_ID, IMAGE_CATALOG_NAME, STACK_NAME, false);

        assertEquals(IMAGE_BASE_ID, images.getBaseImages().get(0).getUuid());
        verify(imageCatalogService).getImages(eq(ORG_ID), eq(IMAGE_CATALOG_NAME), isNull(), eq(AWS), eq(false), eq(null));
        verify(componentConfigProviderService).getImage(STACK_ID);
        verify(stackImageUpdateService).isValidImage(eq(stack), eq(IMAGE_BASE_ID), eq(IMAGE_CATALOG_NAME), eq(CUSTOM_IMAGE_CATALOG_URL));
        verify(stackImageUpdateService, never()).isValidImage(eq(stack), eq(IMAGE_HDP_ID), eq(IMAGE_CATALOG_NAME), eq(CUSTOM_IMAGE_CATALOG_URL));
        verify(stackImageUpdateService, never()).isValidImage(eq(stack), eq(IMAGE_HDF_ID), eq(IMAGE_CATALOG_NAME), eq(CUSTOM_IMAGE_CATALOG_URL));
    }

    @Test
    void testGetApplicableImagesWhenStackImageUpdateServiceRejectsAll() throws CloudbreakImageCatalogException, CloudbreakImageNotFoundException {
        Stack stack = getStack(DetailedStackStatus.AVAILABLE);
        setupLoggedInUser();
        when(imageCatalogService.getImages(anyLong(), anyString(), any(), any(), anyBoolean(), eq(null))).thenReturn(getStatedImages());
        when(stackService.getByNameInWorkspaceWithLists(eq(STACK_NAME), eq(ORG_ID))).thenReturn(Optional.ofNullable(stack));
        when(stackImageUpdateService.isValidImage(any(), anyString(), anyString(), anyString())).thenReturn(false);
        when(componentConfigProviderService.getImage(anyLong())).thenReturn(getImage(""));
        when(platformStringTransformer.getPlatformStringForImageCatalog(anyString(), anyString())).thenReturn(AWS);

        Images images = underTest.getApplicableImages(ORG_ID, IMAGE_CATALOG_NAME, STACK_NAME, true);

        MatcherAssert.assertThat(images.getBaseImages(), empty());
        verify(imageCatalogService).getImages(eq(ORG_ID), eq(IMAGE_CATALOG_NAME), isNull(), eq(AWS), eq(true), eq(null));
        verify(componentConfigProviderService).getImage(STACK_ID);
        verify(stackImageUpdateService).isValidImage(eq(stack), eq(IMAGE_BASE_ID), eq(IMAGE_CATALOG_NAME), eq(CUSTOM_IMAGE_CATALOG_URL));
    }

    @Test
    void testGetApplicableImagesWhenStackNotInAvailableState() throws CloudbreakImageCatalogException {
        Stack stack = getStack(DetailedStackStatus.UPSCALE_IN_PROGRESS);
        when(stackService.getByNameInWorkspaceWithLists(eq(STACK_NAME), eq(ORG_ID))).thenReturn(Optional.ofNullable(stack));
        when(platformStringTransformer.getPlatformStringForImageCatalog(anyString(), anyString())).thenReturn(AWS);

        setupLoggedInUser();

        assertThrows(BadRequestException.class, () -> underTest.getApplicableImages(ORG_ID, IMAGE_CATALOG_NAME, STACK_NAME, false),
                "To retrieve list of images for upgrade cluster have to be in AVAILABLE state");
    }

    @Test
    void testGetApplicableImagesWhenClusterNotInAvailableState() throws CloudbreakImageCatalogException {
        Stack stack = getStack(DetailedStackStatus.CLUSTER_UPGRADE_FAILED);
        when(stackService.getByNameInWorkspaceWithLists(eq(STACK_NAME), eq(ORG_ID))).thenReturn(Optional.ofNullable(stack));
        when(platformStringTransformer.getPlatformStringForImageCatalog(anyString(), anyString())).thenReturn(AWS);

        setupLoggedInUser();

        assertThrows(BadRequestException.class, () -> underTest.getApplicableImages(ORG_ID, IMAGE_CATALOG_NAME, STACK_NAME, false),
                "To retrieve list of images for upgrade cluster have to be in AVAILABLE state");
    }

    private StatedImages getStatedImages() {
        Images images = new Images(
                Collections.singletonList(getImage("a", IMAGE_BASE_ID)),
                Collections.singletonList(getImage("b", IMAGE_CDH_ID)),
                Collections.emptyList(),
                new HashSet<>()
        );
        return StatedImages.statedImages(images, CUSTOM_IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME);
    }

    private Image getImage(String os, String id) {
        Map<String, Map<String, String>> imageSetsByProvider = new HashMap<>();
        imageSetsByProvider.put(PROVIDER_AWS, null);
        return Image.builder()
                .withOs(os)
                .withUuid(id)
                .withImageSetsByProvider(imageSetsByProvider)
                .withAdvertised(true)
                .build();
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image getImage(String id) {
        return com.sequenceiq.cloudbreak.cloud.model.Image.builder().withImageId(id).build();
    }

    private Stack getStack(DetailedStackStatus detailedStackStatus) {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setCloudPlatform(PROVIDER_AWS);
        stack.setPlatformVariant(PROVIDER_AWS);
        stack.setName(STACK_NAME);
        stack.setStackStatus(new StackStatus(stack, detailedStackStatus.getStatus(), "", detailedStackStatus));
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        return stack;
    }

    private CloudbreakUser setupLoggedInUser() {
        CloudbreakUser user = new CloudbreakUser("", "", "", "", "");
        lenient().when(authenticatedUserService.getCbUser(any())).thenReturn(user);
        return user;
    }
}
