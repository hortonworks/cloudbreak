package com.sequenceiq.cloudbreak.service.image;

import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.stack.StackService;

public class StackImageFilterServiceTest {

    private static final String CUSTOM_IMAGE_CATALOG_URL = "http://localhost/custom-imagecatalog-url";

    private static final String PROVIDER_AWS = "AWS";

    private static final String STACK_NAME = "stackName";

    private static final String IMAGE_CATALOG_NAME = "anyImageCatalog";

    private static final String IMAGE_HDP_ID = "hdp-1";

    private static final String IMAGE_BASE_ID = "base-2";

    private static final String IMAGE_HDF_ID = "hdf-3";

    private static final long STACK_ID = 1L;

    private static final long ORG_ID = 100L;

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private StackService stackService;

    @Mock
    private StackImageUpdateService stackImageUpdateService;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private ComponentConfigProvider componentConfigProvider;

    @InjectMocks
    private StackImageFilterService underTest;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetApplicableImagesHdp() throws CloudbreakImageCatalogException, IOException, CloudbreakImageNotFoundException {
        Stack stack = getStack(Status.AVAILABLE, Status.AVAILABLE);
        setupLoggedInUser();
        when(imageCatalogService.getImages(anyLong(), anyString(), anyString())).thenReturn(getStatedImages());
        when(stackService.getByNameInWorkspaceWithLists(eq(STACK_NAME), eq(ORG_ID))).thenReturn(stack);
        when(stackImageUpdateService.isValidImage(any(), anyString(), anyString(), anyString())).thenReturn(true);
        when(stackImageUpdateService.getStackType(any())).thenReturn(StackType.HDP);
        when(componentConfigProvider.getImage(anyLong())).thenReturn(getImage(""));

        Images images = underTest.getApplicableImages(ORG_ID, IMAGE_CATALOG_NAME, STACK_NAME);

        assertEquals(IMAGE_BASE_ID, images.getBaseImages().get(0).getUuid());
        assertEquals(IMAGE_HDP_ID, images.getHdpImages().get(0).getUuid());
        assertThat(images.getHdfImages(), empty());
        verify(imageCatalogService).getImages(eq(ORG_ID), eq(IMAGE_CATALOG_NAME), eq(PROVIDER_AWS));
        verify(componentConfigProvider).getImage(STACK_ID);
        verify(stackImageUpdateService).isValidImage(eq(stack), eq(IMAGE_BASE_ID), eq(IMAGE_CATALOG_NAME), eq(CUSTOM_IMAGE_CATALOG_URL));
        verify(stackImageUpdateService).isValidImage(eq(stack), eq(IMAGE_HDP_ID), eq(IMAGE_CATALOG_NAME), eq(CUSTOM_IMAGE_CATALOG_URL));
        verify(stackImageUpdateService, never()).isValidImage(eq(stack), eq(IMAGE_HDF_ID), eq(IMAGE_CATALOG_NAME), eq(CUSTOM_IMAGE_CATALOG_URL));
    }

    @Test
    public void testGetApplicableImagesHdf() throws CloudbreakImageCatalogException, IOException, CloudbreakImageNotFoundException {
        Stack stack = getStack(Status.AVAILABLE, Status.AVAILABLE);
        setupLoggedInUser();
        when(imageCatalogService.getImages(anyLong(), anyString(), anyString())).thenReturn(getStatedImages());
        when(stackService.getByNameInWorkspaceWithLists(eq(STACK_NAME), eq(ORG_ID))).thenReturn(stack);
        when(stackImageUpdateService.isValidImage(any(), anyString(), anyString(), anyString())).thenReturn(true);
        when(stackImageUpdateService.getStackType(any())).thenReturn(StackType.HDF);
        when(componentConfigProvider.getImage(anyLong())).thenReturn(getImage(""));

        Images images = underTest.getApplicableImages(ORG_ID, IMAGE_CATALOG_NAME, STACK_NAME);

        assertEquals(IMAGE_BASE_ID, images.getBaseImages().get(0).getUuid());
        assertThat(images.getHdpImages(), empty());
        assertEquals(IMAGE_HDF_ID, images.getHdfImages().get(0).getUuid());
        verify(imageCatalogService).getImages(eq(ORG_ID), eq(IMAGE_CATALOG_NAME), eq(PROVIDER_AWS));
        verify(componentConfigProvider).getImage(STACK_ID);
        verify(stackImageUpdateService).isValidImage(eq(stack), eq(IMAGE_BASE_ID), eq(IMAGE_CATALOG_NAME), eq(CUSTOM_IMAGE_CATALOG_URL));
        verify(stackImageUpdateService, never()).isValidImage(eq(stack), eq(IMAGE_HDP_ID), eq(IMAGE_CATALOG_NAME), eq(CUSTOM_IMAGE_CATALOG_URL));
        verify(stackImageUpdateService).isValidImage(eq(stack), eq(IMAGE_HDF_ID), eq(IMAGE_CATALOG_NAME), eq(CUSTOM_IMAGE_CATALOG_URL));
    }

    @Test
    public void testGetApplicableImagesFiltersCurrentImage() throws CloudbreakImageCatalogException, IOException, CloudbreakImageNotFoundException {
        Stack stack = getStack(Status.AVAILABLE, Status.AVAILABLE);
        setupLoggedInUser();
        when(imageCatalogService.getImages(anyLong(), anyString(), anyString())).thenReturn(getStatedImages());
        when(stackService.getByNameInWorkspaceWithLists(eq(STACK_NAME), eq(ORG_ID))).thenReturn(stack);
        when(stackImageUpdateService.isValidImage(any(), anyString(), anyString(), anyString())).thenReturn(true);
        when(stackImageUpdateService.getStackType(any())).thenReturn(StackType.HDP);
        when(componentConfigProvider.getImage(anyLong())).thenReturn(getImage(IMAGE_HDP_ID));

        Images images = underTest.getApplicableImages(ORG_ID, IMAGE_CATALOG_NAME, STACK_NAME);

        assertEquals(IMAGE_BASE_ID, images.getBaseImages().get(0).getUuid());
        assertThat(images.getHdpImages(), empty());
        assertThat(images.getHdfImages(), empty());
        verify(imageCatalogService).getImages(eq(ORG_ID), eq(IMAGE_CATALOG_NAME), eq(PROVIDER_AWS));
        verify(componentConfigProvider).getImage(STACK_ID);
        verify(stackImageUpdateService).isValidImage(eq(stack), eq(IMAGE_BASE_ID), eq(IMAGE_CATALOG_NAME), eq(CUSTOM_IMAGE_CATALOG_URL));
        verify(stackImageUpdateService, never()).isValidImage(eq(stack), eq(IMAGE_HDP_ID), eq(IMAGE_CATALOG_NAME), eq(CUSTOM_IMAGE_CATALOG_URL));
        verify(stackImageUpdateService, never()).isValidImage(eq(stack), eq(IMAGE_HDF_ID), eq(IMAGE_CATALOG_NAME), eq(CUSTOM_IMAGE_CATALOG_URL));
    }

    @Test
    public void testGetApplicableImagesWhenStackImageUpdateServiceRejectsAll() throws CloudbreakImageCatalogException, CloudbreakImageNotFoundException {
        Stack stack = getStack(Status.AVAILABLE, Status.AVAILABLE);
        setupLoggedInUser();
        when(imageCatalogService.getImages(anyLong(), anyString(), anyString())).thenReturn(getStatedImages());
        when(stackService.getByNameInWorkspaceWithLists(eq(STACK_NAME), eq(ORG_ID))).thenReturn(stack);
        when(stackImageUpdateService.isValidImage(any(), anyString(), anyString(), anyString())).thenReturn(false);
        when(stackImageUpdateService.getStackType(any())).thenReturn(StackType.HDP);
        when(componentConfigProvider.getImage(anyLong())).thenReturn(getImage(""));

        Images images = underTest.getApplicableImages(ORG_ID, IMAGE_CATALOG_NAME, STACK_NAME);

        assertThat(images.getHdpImages(), empty());
        assertThat(images.getHdfImages(), empty());
        assertThat(images.getBaseImages(), empty());
        verify(imageCatalogService).getImages(eq(ORG_ID), eq(IMAGE_CATALOG_NAME), eq(PROVIDER_AWS));
        verify(componentConfigProvider).getImage(STACK_ID);
        verify(stackImageUpdateService).isValidImage(eq(stack), eq(IMAGE_BASE_ID), eq(IMAGE_CATALOG_NAME), eq(CUSTOM_IMAGE_CATALOG_URL));
        verify(stackImageUpdateService).isValidImage(eq(stack), eq(IMAGE_HDP_ID), eq(IMAGE_CATALOG_NAME), eq(CUSTOM_IMAGE_CATALOG_URL));
        verify(stackImageUpdateService, never()).isValidImage(eq(stack), eq(IMAGE_HDF_ID), eq(IMAGE_CATALOG_NAME), eq(CUSTOM_IMAGE_CATALOG_URL));
    }

    @Test
    public void testGetApplicableImagesWhenStackNotInAvailableState() throws CloudbreakImageCatalogException, IOException {
        Stack stack = getStack(Status.CREATE_IN_PROGRESS, Status.AVAILABLE);
        when(stackService.getByNameInWorkspaceWithLists(eq(STACK_NAME), eq(ORG_ID))).thenReturn(stack);
        setupLoggedInUser();
        thrown.expectMessage("To retrieve list of images for upgrade both stack and cluster have to be in AVAILABLE state");
        thrown.expect(BadRequestException.class);

        underTest.getApplicableImages(ORG_ID, IMAGE_CATALOG_NAME, STACK_NAME);
    }

    @Test
    public void testGetApplicableImagesWhenClusterNotInAvailableState() throws CloudbreakImageCatalogException, IOException {
        Stack stack = getStack(Status.AVAILABLE, Status.UPDATE_FAILED);
        when(stackService.getByNameInWorkspaceWithLists(eq(STACK_NAME), eq(ORG_ID))).thenReturn(stack);
        setupLoggedInUser();
        thrown.expectMessage("To retrieve list of images for upgrade both stack and cluster have to be in AVAILABLE state");
        thrown.expect(BadRequestException.class);

        underTest.getApplicableImages(ORG_ID, IMAGE_CATALOG_NAME, STACK_NAME);
    }

    private StatedImages getStatedImages() {
        Images images = new Images(
                Collections.singletonList(getImage("a", IMAGE_BASE_ID)),
                Collections.singletonList(getImage("b", IMAGE_HDP_ID)),
                Collections.singletonList(getImage("c", IMAGE_HDF_ID)),
                new HashSet<>()
        );
        return StatedImages.statedImages(images, CUSTOM_IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME);
    }

    private Image getImage(String os, String id) {
        Map<String, Map<String, String>> imageSetsByProvider = new HashMap<>();
        imageSetsByProvider.put(PROVIDER_AWS, null);
        return new Image("", "", os, id, "", Collections.emptyMap(), imageSetsByProvider, null, "", Collections.emptyMap());
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image getImage(String id) {
        Map<String, Map<String, String>> imageSetsByProvider = new HashMap<>();
        imageSetsByProvider.put(PROVIDER_AWS, null);
        return new com.sequenceiq.cloudbreak.cloud.model.Image("", Collections.emptyMap(), "", "", "", "", id, Collections.emptyMap());
    }

    private Stack getStack(Status stackStatus, Status clusterStatus) {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setCloudPlatform(PROVIDER_AWS);
        stack.setName(STACK_NAME);
        stack.setStackStatus(new StackStatus(stack, stackStatus, "", DetailedStackStatus.UNKNOWN));
        Cluster cluster = new Cluster();
        cluster.setStatus(clusterStatus);
        stack.setCluster(cluster);
        return stack;
    }

    private CloudbreakUser setupLoggedInUser() {
        CloudbreakUser user = new CloudbreakUser("", "", "", "");
        when(authenticatedUserService.getCbUser()).thenReturn(user);
        return user;
    }
}
