package com.sequenceiq.cloudbreak.core.flow2.stack.image.update;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.CheckResult;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
public class StackImageUpdateServiceTest {

    private static final Long WORKSPACE_ID = 123L;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private ImageService imageService;

    @Mock
    private StackImageService stackImageService;

    @Mock
    private PackageVersionChecker packageVersionChecker;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @InjectMocks
    private StackImageUpdateService underTest;

    private Stack stack;

    private StatedImage statedImage;

    private Image image;

    private final Map<String, String> packageVersions = Collections.singletonMap("package", "version");

    @BeforeEach
    public void setUp() {
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);

        stack = new Stack();
        stack.setId(1L);
        stack.setName("stackname");
        stack.setRegion("region");
        stack.setCloudPlatform("AWS");
        stack.setWorkspace(workspace);

        image = new Image("asdf", System.currentTimeMillis(), System.currentTimeMillis(), "asdf", "centos7", "uuid", "2.8.0", Collections.emptyMap(),
                Collections.singletonMap("AWS", Collections.emptyMap()), null, "centos", packageVersions,
                Collections.emptyList(), Collections.emptyList(), "1", true, null, null);
        statedImage = StatedImage.statedImage(image, "url", "name");
        lenient().when(packageVersionChecker.checkInstancesHaveAllMandatoryPackageVersion(anyList())).thenReturn(CheckResult.ok());
        lenient().when(packageVersionChecker.checkInstancesHaveMultiplePackageVersions(anyList())).thenReturn(CheckResult.ok());
        lenient().when(packageVersionChecker.compareImageAndInstancesMandatoryPackageVersion(any(StatedImage.class), anyList())).thenReturn(CheckResult.ok());
        lenient().when(messagesService.getMessage(anyString(), anyCollection())).thenReturn("message");
    }

    @Test
    public void testIsCbVersionOk() {
        CloudbreakDetails cloudbreakDetails = new CloudbreakDetails();
        cloudbreakDetails.setVersion(StackImageUpdateService.MIN_VERSION);
        when(componentConfigProviderService.getCloudbreakDetails(stack.getId())).thenReturn(cloudbreakDetails);
        assertTrue(underTest.isCbVersionOk(stack));
        cloudbreakDetails.setVersion("2.6.0");
        assertFalse(underTest.isCbVersionOk(stack));
    }

    @Test
    public void testGetNewImageIfVersionsMatch() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent =
                new com.sequenceiq.cloudbreak.cloud.model.Image("imageOldName", Collections.emptyMap(), "centos7", "centos",
                        statedImage.getImageCatalogUrl(), statedImage.getImageCatalogName(), "uuid2", packageVersions);
        when(stackImageService.getCurrentImage(anyLong())).thenReturn(imageInComponent);
        when(imageCatalogService.getImage(anyLong(), anyString(), anyString(), anyString())).thenReturn(statedImage);

        StatedImage newImageIfVersionsMatch = underTest.getNewImageIfVersionsMatch(stack, "newimageid", "imagecatalogname", "imagecatalogurl");
        assertNotNull(newImageIfVersionsMatch);
        verify(restRequestThreadLocalService).setWorkspaceId(stack.getWorkspaceId());
    }

    @Test
    public void testGetNewImageIfOsVersionsMatchFail() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent =
                new com.sequenceiq.cloudbreak.cloud.model.Image("imageOldName", Collections.emptyMap(), "centos6", "centos",
                        statedImage.getImageCatalogUrl(), statedImage.getImageCatalogName(), "uuid2", packageVersions);
        when(stackImageService.getCurrentImage(stack.getId())).thenReturn(imageInComponent);
        when(imageCatalogService.getImage(anyLong(), anyString(), anyString(), anyString())).thenReturn(statedImage);
        assertThrows(OperationException.class, () -> underTest.getNewImageIfVersionsMatch(stack, "newimageid", "imagecatalogname", "imagecatalogurl"));

        verify(restRequestThreadLocalService).setWorkspaceId(stack.getWorkspaceId());
    }

    @Test
    public void testGetNewImageIfCloudPlatformMatchFail() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        stack.setCloudPlatform("GCP");
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent =
                new com.sequenceiq.cloudbreak.cloud.model.Image("imageOldName", Collections.emptyMap(), "centos7", "centos",
                        statedImage.getImageCatalogUrl(), statedImage.getImageCatalogName(), "uuid2", packageVersions);
        when(stackImageService.getCurrentImage(stack.getId())).thenReturn(imageInComponent);
        when(imageCatalogService.getImage(anyLong(), anyString(), anyString(), anyString())).thenReturn(statedImage);
        assertThrows(OperationException.class, () -> underTest.getNewImageIfVersionsMatch(stack, "newimageid", "imagecatalogname", "imagecatalogurl"));

        verify(restRequestThreadLocalService).setWorkspaceId(stack.getWorkspaceId());
    }

    @Test
    public void testIsValidImage() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent =
                new com.sequenceiq.cloudbreak.cloud.model.Image("imageOldName", Collections.emptyMap(), "centos7", "centos",
                        statedImage.getImageCatalogUrl(), statedImage.getImageCatalogName(), "uuid2", packageVersions);
        when(stackImageService.getCurrentImage(stack.getId())).thenReturn(imageInComponent);
        when(imageCatalogService.getImage(anyLong(), anyString(), anyString(), anyString())).thenReturn(statedImage);
        CloudbreakDetails cloudbreakDetails = new CloudbreakDetails();
        cloudbreakDetails.setVersion(StackImageUpdateService.MIN_VERSION);
        when(componentConfigProviderService.getCloudbreakDetails(stack.getId())).thenReturn(cloudbreakDetails);
        boolean validImage = underTest.isValidImage(stack, "imageId", "imageCatalogName", "imageCatalogUrl");
        assertTrue(validImage);
    }

    @Test
    public void testIsValidImageFalse() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent =
                new com.sequenceiq.cloudbreak.cloud.model.Image("imageOldName", Collections.emptyMap(), "centos6", "centos",
                        statedImage.getImageCatalogUrl(), statedImage.getImageCatalogName(), "uuid2", packageVersions);
        when(stackImageService.getCurrentImage(stack.getId())).thenReturn(imageInComponent);
        when(imageCatalogService.getImage(anyLong(), anyString(), anyString(), anyString())).thenReturn(statedImage);
        CloudbreakDetails cloudbreakDetails = new CloudbreakDetails();
        cloudbreakDetails.setVersion("2.9.0");
        when(componentConfigProviderService.getCloudbreakDetails(stack.getId())).thenReturn(cloudbreakDetails);
        boolean validImage = underTest.isValidImage(stack, "imageId", "imageCatalogName", "imageCatalogUrl");
        assertFalse(validImage);
    }

    @Test
    public void testPackageVersionCheckerFailedThenResultShouldBeSkipped() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent =
                new com.sequenceiq.cloudbreak.cloud.model.Image("imageOldName", Collections.emptyMap(), "centos7", "centos",
                        statedImage.getImageCatalogUrl(), statedImage.getImageCatalogName(), "uuid2", packageVersions);

        CloudbreakDetails cloudbreakDetails = new CloudbreakDetails();
        cloudbreakDetails.setVersion(StackImageUpdateService.MIN_VERSION);

        when(componentConfigProviderService.getCloudbreakDetails(stack.getId())).thenReturn(cloudbreakDetails);
        when(stackImageService.getCurrentImage(stack.getId())).thenReturn(imageInComponent);
        when(imageCatalogService.getImage(anyLong(), anyString(), anyString(), anyString())).thenReturn(statedImage);
        when(packageVersionChecker.checkInstancesHaveAllMandatoryPackageVersion(anyList())).thenReturn(CheckResult.failed(
                "Instance ID: instance-id Packages without version: salt"));

        boolean validImage = underTest.isValidImage(stack, "imageId", "imageCatalogName", "imageCatalogUrl");
        assertTrue(validImage);

    }

    @Test
    public void testOsCheckFailedThenResultShouldNotBeSkipped() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent =
                new com.sequenceiq.cloudbreak.cloud.model.Image("imageOldName", Collections.emptyMap(), "redhat7", "redhat",
                        statedImage.getImageCatalogUrl(), statedImage.getImageCatalogName(), "uuid2", packageVersions);

        CloudbreakDetails cloudbreakDetails = new CloudbreakDetails();
        cloudbreakDetails.setVersion(StackImageUpdateService.MIN_VERSION);

        when(componentConfigProviderService.getCloudbreakDetails(stack.getId())).thenReturn(cloudbreakDetails);
        when(stackImageService.getCurrentImage(stack.getId())).thenReturn(imageInComponent);
        when(imageCatalogService.getImage(anyLong(), anyString(), anyString(), anyString())).thenReturn(statedImage);
        when(packageVersionChecker.checkInstancesHaveAllMandatoryPackageVersion(anyList())).thenReturn(CheckResult.ok());

        boolean validImage = underTest.isValidImage(stack, "imageId", "imageCatalogName", "imageCatalogUrl");
        assertFalse(validImage);

    }
}