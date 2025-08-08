package com.sequenceiq.cloudbreak.core.flow2.stack.image.update;

import static com.sequenceiq.common.model.ImageCatalogPlatform.imageCatalogPlatform;
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
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.CheckResult;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.service.upgrade.image.OsChangeUpgradeCondition;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.OsType;

@ExtendWith(MockitoExtension.class)
public class StackImageUpdateServiceTest {

    private static final Long WORKSPACE_ID = 123L;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private StackImageService stackImageService;

    @Mock
    private PackageVersionChecker packageVersionChecker;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private PlatformStringTransformer platformStringTransformer;

    @Mock
    private OsChangeUpgradeCondition osChangeUpgradeCondition;

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
        stack.setPlatformVariant("AWS_NATIVE");
        stack.setWorkspace(workspace);

        image = Image.builder()
                .withOs("centos7")
                .withOsType("centos")
                .withImageSetsByProvider(Collections.singletonMap("AWS", Collections.emptyMap()))
                .withPackageVersions(packageVersions)
                .withAdvertised(true)
                .build();
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
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent1 = com.sequenceiq.cloudbreak.cloud.model.Image.builder()
                .withImageName("imageOldName")
                .withImageId("uuid2")
                .withOs("centos7")
                .withOsType("centos")
                .withArchitecture(Architecture.X86_64.getName())
                .withPackageVersions(packageVersions)
                .withImageCatalogUrl(statedImage.getImageCatalogUrl())
                .withImageCatalogName(statedImage.getImageCatalogName())
                .build();
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent = imageInComponent1;
        when(stackImageService.getCurrentImage(anyLong())).thenReturn(imageInComponent);
        when(imageCatalogService.getImage(anyLong(), anyString(), anyString(), anyString())).thenReturn(statedImage);
        when(platformStringTransformer.getPlatformStringForImageCatalog(anyString(), anyString())).thenReturn(imageCatalogPlatform("AWS"));

        StatedImage newImageIfVersionsMatch = underTest.getNewImageIfVersionsMatch(stack, "newimageid", "imagecatalogname", "imagecatalogurl");
        assertNotNull(newImageIfVersionsMatch);
        verify(restRequestThreadLocalService).setWorkspaceId(stack.getWorkspaceId());
    }

    @Test
    public void testGetNewImageIfOsVersionsMatchFail() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent1 = com.sequenceiq.cloudbreak.cloud.model.Image.builder()
                .withImageName("imageOldName")
                .withImageId("uuid2")
                .withOs("centos6")
                .withOsType("centos")
                .withArchitecture(Architecture.X86_64.getName())
                .withPackageVersions(packageVersions)
                .withImageCatalogUrl(statedImage.getImageCatalogUrl())
                .withImageCatalogName(statedImage.getImageCatalogName())
                .build();
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent = imageInComponent1;
        when(stackImageService.getCurrentImage(stack.getId())).thenReturn(imageInComponent);
        when(imageCatalogService.getImage(anyLong(), anyString(), anyString(), anyString())).thenReturn(statedImage);
        when(platformStringTransformer.getPlatformStringForImageCatalog(anyString(), anyString())).thenReturn(imageCatalogPlatform("AWS"));

        assertThrows(OperationException.class, () -> underTest.getNewImageIfVersionsMatch(stack, "newimageid", "imagecatalogname", "imagecatalogurl"));

        verify(restRequestThreadLocalService).setWorkspaceId(stack.getWorkspaceId());
    }

    @Test
    public void testGetNewImageIfOsVersionsMatchShouldNotThrowExceptionWhenCentOSToRedhatOSUpgradePermitted()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        StatedImage targetStatedImage = StatedImage.statedImage(Image.builder()
                        .withOs(OsType.RHEL8.getOs())
                        .withOsType(OsType.RHEL8.getOsType())
                        .withImageSetsByProvider(image.getImageSetsByProvider())
                        .withStackDetails(image.getStackDetails()).build(),
                statedImage.getImageCatalogUrl(), statedImage.getImageCatalogName());
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent1 = com.sequenceiq.cloudbreak.cloud.model.Image.builder()
                .withImageName("imageOldName")
                .withImageId("uuid2")
                .withOs("centos7")
                .withOsType("redhat7")
                .withArchitecture(Architecture.X86_64.getName())
                .withPackageVersions(packageVersions)
                .withImageCatalogUrl(statedImage.getImageCatalogUrl())
                .withImageCatalogName(statedImage.getImageCatalogName())
                .build();
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent = imageInComponent1;
        when(stackImageService.getCurrentImage(stack.getId())).thenReturn(imageInComponent);
        when(imageCatalogService.getImage(anyLong(), anyString(), anyString(), anyString())).thenReturn(targetStatedImage);
        when(platformStringTransformer.getPlatformStringForImageCatalog(anyString(), anyString())).thenReturn(imageCatalogPlatform("AWS"));
        when(osChangeUpgradeCondition.isNextMajorOsImage(stack.getId(), targetStatedImage.getImage())).thenReturn(true);

        underTest.getNewImageIfVersionsMatch(stack, "newimageid", "imagecatalogname", "imagecatalogurl");

        verify(restRequestThreadLocalService).setWorkspaceId(stack.getWorkspaceId());
    }

    @Test
    public void testGetNewImageIfCloudPlatformMatchFail() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        stack.setCloudPlatform("GCP");
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent1 = com.sequenceiq.cloudbreak.cloud.model.Image.builder()
                .withImageName("imageOldName")
                .withImageId("uuid2")
                .withOs("centos7")
                .withOsType("centos")
                .withArchitecture(Architecture.X86_64.getName())
                .withPackageVersions(packageVersions)
                .withImageCatalogUrl(statedImage.getImageCatalogUrl())
                .withImageCatalogName(statedImage.getImageCatalogName())
                .build();
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent = imageInComponent1;
        when(stackImageService.getCurrentImage(stack.getId())).thenReturn(imageInComponent);
        when(imageCatalogService.getImage(anyLong(), anyString(), anyString(), anyString())).thenReturn(statedImage);
        when(platformStringTransformer.getPlatformStringForImageCatalog(anyString(), anyString())).thenReturn(imageCatalogPlatform("GCP"));

        assertThrows(OperationException.class, () -> underTest.getNewImageIfVersionsMatch(stack, "newimageid", "imagecatalogname", "imagecatalogurl"));

        verify(restRequestThreadLocalService).setWorkspaceId(stack.getWorkspaceId());
    }

    @Test
    public void testGetNewImageIfCloudPlatformAwsGov() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        stack.setPlatformVariant("AWS_GOV_NATIVE");
        image = Image.builder()
                .copy(image)
                .withImageSetsByProvider(Collections.singletonMap("AWS_GOV", Collections.emptyMap()))
                .build();
        statedImage = StatedImage.statedImage(image, "url", "name");
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent1 = com.sequenceiq.cloudbreak.cloud.model.Image.builder()
                .withImageName("imageOldName")
                .withImageId("uuid2")
                .withOs("centos7")
                .withOsType("centos")
                .withArchitecture(Architecture.X86_64.getName())
                .withPackageVersions(packageVersions)
                .withImageCatalogUrl(statedImage.getImageCatalogUrl())
                .withImageCatalogName(statedImage.getImageCatalogName())
                .build();
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent = imageInComponent1;
        when(stackImageService.getCurrentImage(stack.getId())).thenReturn(imageInComponent);
        when(imageCatalogService.getImage(anyLong(), anyString(), anyString(), anyString())).thenReturn(statedImage);
        when(platformStringTransformer.getPlatformStringForImageCatalog(anyString(), anyString())).thenReturn(imageCatalogPlatform("aws_gov"));

        CloudbreakDetails cloudbreakDetails = new CloudbreakDetails();
        cloudbreakDetails.setVersion(StackImageUpdateService.MIN_VERSION);
        when(componentConfigProviderService.getCloudbreakDetails(stack.getId())).thenReturn(cloudbreakDetails);
        boolean validImage = underTest.isValidImage(stack, "imageId", "imageCatalogName", "imageCatalogUrl");
        assertTrue(validImage);
    }

    @Test
    public void testIsValidImage() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent1 = com.sequenceiq.cloudbreak.cloud.model.Image.builder()
                .withImageName("imageOldName")
                .withImageId("uuid2")
                .withOs("centos7")
                .withOsType("centos")
                .withArchitecture(Architecture.X86_64.getName())
                .withPackageVersions(packageVersions)
                .withImageCatalogUrl(statedImage.getImageCatalogUrl())
                .withImageCatalogName(statedImage.getImageCatalogName())
                .build();
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent = imageInComponent1;
        when(stackImageService.getCurrentImage(stack.getId())).thenReturn(imageInComponent);
        when(imageCatalogService.getImage(anyLong(), anyString(), anyString(), anyString())).thenReturn(statedImage);
        when(platformStringTransformer.getPlatformStringForImageCatalog(anyString(), anyString())).thenReturn(imageCatalogPlatform("AWS"));
        CloudbreakDetails cloudbreakDetails = new CloudbreakDetails();
        cloudbreakDetails.setVersion(StackImageUpdateService.MIN_VERSION);
        when(componentConfigProviderService.getCloudbreakDetails(stack.getId())).thenReturn(cloudbreakDetails);
        boolean validImage = underTest.isValidImage(stack, "imageId", "imageCatalogName", "imageCatalogUrl");
        assertTrue(validImage);
    }

    @Test
    public void testIsValidImageFalse() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent1 = com.sequenceiq.cloudbreak.cloud.model.Image.builder()
                .withImageName("imageOldName")
                .withImageId("uuid2")
                .withOs("centos6")
                .withOsType("centos")
                .withArchitecture(Architecture.X86_64.getName())
                .withPackageVersions(packageVersions)
                .withImageCatalogUrl(statedImage.getImageCatalogUrl())
                .withImageCatalogName(statedImage.getImageCatalogName())
                .build();
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent = imageInComponent1;
        when(stackImageService.getCurrentImage(stack.getId())).thenReturn(imageInComponent);
        when(imageCatalogService.getImage(anyLong(), anyString(), anyString(), anyString())).thenReturn(statedImage);
        when(platformStringTransformer.getPlatformStringForImageCatalog(anyString(), anyString())).thenReturn(imageCatalogPlatform("AWS"));
        CloudbreakDetails cloudbreakDetails = new CloudbreakDetails();
        cloudbreakDetails.setVersion("2.9.0");
        when(componentConfigProviderService.getCloudbreakDetails(stack.getId())).thenReturn(cloudbreakDetails);
        boolean validImage = underTest.isValidImage(stack, "imageId", "imageCatalogName", "imageCatalogUrl");
        assertFalse(validImage);
    }

    @Test
    public void testPackageVersionCheckerFailedThenResultShouldBeSkipped() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent1 = com.sequenceiq.cloudbreak.cloud.model.Image.builder()
                .withImageName("imageOldName")
                .withImageId("uuid2")
                .withOs("centos7")
                .withOsType("centos")
                .withArchitecture(Architecture.X86_64.getName())
                .withPackageVersions(packageVersions)
                .withImageCatalogUrl(statedImage.getImageCatalogUrl())
                .withImageCatalogName(statedImage.getImageCatalogName())
                .build();
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent = imageInComponent1;

        CloudbreakDetails cloudbreakDetails = new CloudbreakDetails();
        cloudbreakDetails.setVersion(StackImageUpdateService.MIN_VERSION);

        when(componentConfigProviderService.getCloudbreakDetails(stack.getId())).thenReturn(cloudbreakDetails);
        when(stackImageService.getCurrentImage(stack.getId())).thenReturn(imageInComponent);
        when(imageCatalogService.getImage(anyLong(), anyString(), anyString(), anyString())).thenReturn(statedImage);
        when(packageVersionChecker.checkInstancesHaveAllMandatoryPackageVersion(anyList())).thenReturn(CheckResult.failed(
                "Instance ID: instance-id Packages without version: salt"));
        when(platformStringTransformer.getPlatformStringForImageCatalog(anyString(), anyString())).thenReturn(imageCatalogPlatform("AWS"));

        boolean validImage = underTest.isValidImage(stack, "imageId", "imageCatalogName", "imageCatalogUrl");
        assertTrue(validImage);

    }

    @Test
    public void testOsCheckFailedThenResultShouldNotBeSkipped() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent1 = com.sequenceiq.cloudbreak.cloud.model.Image.builder()
                .withImageName("imageOldName")
                .withImageId("uuid2")
                .withOs("redhat7")
                .withOsType("redhat")
                .withArchitecture(Architecture.X86_64.getName())
                .withPackageVersions(packageVersions)
                .withImageCatalogUrl(statedImage.getImageCatalogUrl())
                .withImageCatalogName(statedImage.getImageCatalogName())
                .build();
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent = imageInComponent1;

        CloudbreakDetails cloudbreakDetails = new CloudbreakDetails();
        cloudbreakDetails.setVersion(StackImageUpdateService.MIN_VERSION);

        when(componentConfigProviderService.getCloudbreakDetails(stack.getId())).thenReturn(cloudbreakDetails);
        when(stackImageService.getCurrentImage(stack.getId())).thenReturn(imageInComponent);
        when(imageCatalogService.getImage(anyLong(), anyString(), anyString(), anyString())).thenReturn(statedImage);
        when(packageVersionChecker.checkInstancesHaveAllMandatoryPackageVersion(anyList())).thenReturn(CheckResult.ok());
        when(platformStringTransformer.getPlatformStringForImageCatalog(anyString(), anyString())).thenReturn(imageCatalogPlatform("AWS"));

        boolean validImage = underTest.isValidImage(stack, "imageId", "imageCatalogName", "imageCatalogUrl");
        assertFalse(validImage);

    }
}