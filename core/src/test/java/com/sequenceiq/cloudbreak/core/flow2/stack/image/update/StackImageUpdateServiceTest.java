package com.sequenceiq.cloudbreak.core.flow2.stack.image.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.CheckResult;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException;

public class StackImageUpdateServiceTest {

    @Mock
    private ComponentConfigProvider componentConfigProvider;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private ImageService imageService;

    @Mock
    private StackService stackService;

    @Mock
    private PackageVersionChecker packageVersionChecker;

    @Mock
    private CloudbreakMessagesService messagesService;

    @InjectMocks
    private StackImageUpdateService underTest;

    private Stack stack;

    private StatedImage statedImage;

    private Image image;

    private Map<String, String> packageVersions = Collections.singletonMap("package", "version");

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        stack = new Stack();
        stack.setId(1L);
        stack.setName("stackname");
        stack.setRegion("region");
        stack.setCloudPlatform("AWS");

        image = new Image("asdf", "asdf", "centos7", "uuid", "2.8.0", Collections.emptyMap(),
                Collections.singletonMap("AWS", Collections.emptyMap()), null, "centos", packageVersions);
        statedImage = StatedImage.statedImage(image, "url", "name");
        when(packageVersionChecker.checkInstancesHaveAllMandatoryPackageVersion(anySet())).thenReturn(CheckResult.ok());
        when(packageVersionChecker.checkInstancesHaveMultiplePackageVersions(anySet())).thenReturn(CheckResult.ok());
        when(packageVersionChecker.compareImageAndInstancesMandatoryPackageVersion(any(StatedImage.class), anySet())).thenReturn(CheckResult.ok());
        when(messagesService.getMessage(anyString(), anyCollection())).thenReturn("message");
    }

    @Test
    public void teststoreNewImageComponent() throws CloudbreakImageNotFoundException, IOException {

        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent =
                new com.sequenceiq.cloudbreak.cloud.model.Image("imageOldName", Collections.emptyMap(), image.getOs(), image.getOsType(),
                        statedImage.getImageCatalogUrl(), statedImage.getImageCatalogName(), "uuid2", packageVersions);
        when(componentConfigProvider.getImage(anyLong())).thenReturn(imageInComponent);

        String imagename = "imagename";
        when(imageService.determineImageName(anyString(), anyString(), eq(image))).thenReturn(imagename);

        underTest.storeNewImageComponent(stack, statedImage);

        ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        verify(componentConfigProvider).replaceImageComponentWithNew(captor.capture());
        assertEquals(ComponentType.IMAGE, captor.getValue().getComponentType());
        assertEquals(ComponentType.IMAGE.name(), captor.getValue().getName());
        assertEquals(imagename, captor.getValue().getAttributes().get(com.sequenceiq.cloudbreak.cloud.model.Image.class).getImageName());
        assertEquals(image.getUuid(), captor.getValue().getAttributes().get(com.sequenceiq.cloudbreak.cloud.model.Image.class).getImageId());
    }

    @Test
    public void testIsCbVersionOk() {
        CloudbreakDetails cloudbreakDetails = new CloudbreakDetails();
        cloudbreakDetails.setVersion(StackImageUpdateService.MIN_VERSION);
        when(componentConfigProvider.getCloudbreakDetails(stack.getId())).thenReturn(cloudbreakDetails);
        assertTrue(underTest.isCbVersionOk(stack));
        cloudbreakDetails.setVersion("2.6.0");
        assertFalse(underTest.isCbVersionOk(stack));
    }

    @Test
    public void testGetNewImageIfVersionsMatch() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent =
                new com.sequenceiq.cloudbreak.cloud.model.Image("imageOldName", Collections.emptyMap(), "centos7", "centos",
                        statedImage.getImageCatalogUrl(), statedImage.getImageCatalogName(), "uuid2", packageVersions);
        when(componentConfigProvider.getImage(anyLong())).thenReturn(imageInComponent);
        when(imageCatalogService.getImage(anyString(), anyString(), anyString())).thenReturn(statedImage);
        StatedImage newImageIfVersionsMatch = underTest.getNewImageIfVersionsMatch(stack, "newimageid", "imagecatalogname", "imagecatalogurl");
        assertNotNull(newImageIfVersionsMatch);
    }

    @Test(expected = OperationException.class)
    public void testGetNewImageIfOsVersionsMatchFail() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent =
                new com.sequenceiq.cloudbreak.cloud.model.Image("imageOldName", Collections.emptyMap(), "centos6", "centos",
                        statedImage.getImageCatalogUrl(), statedImage.getImageCatalogName(), "uuid2", packageVersions);
        when(componentConfigProvider.getImage(anyLong())).thenReturn(imageInComponent);
        when(imageCatalogService.getImage(anyString(), anyString(), anyString())).thenReturn(statedImage);
        underTest.getNewImageIfVersionsMatch(stack, "newimageid", "imagecatalogname", "imagecatalogurl");
    }

    @Test(expected = OperationException.class)
    public void testGetNewImageIfCloudPlatformMatchFail() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        stack.setCloudPlatform("GCP");
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent =
                new com.sequenceiq.cloudbreak.cloud.model.Image("imageOldName", Collections.emptyMap(), "centos7", "centos",
                        statedImage.getImageCatalogUrl(), statedImage.getImageCatalogName(), "uuid2", packageVersions);
        when(componentConfigProvider.getImage(anyLong())).thenReturn(imageInComponent);
        when(imageCatalogService.getImage(anyString(), anyString(), anyString())).thenReturn(statedImage);
        underTest.getNewImageIfVersionsMatch(stack, "newimageid", "imagecatalogname", "imagecatalogurl");
    }

    @Test
    public void testIsValidImage() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent =
                new com.sequenceiq.cloudbreak.cloud.model.Image("imageOldName", Collections.emptyMap(), "centos7", "centos",
                        statedImage.getImageCatalogUrl(), statedImage.getImageCatalogName(), "uuid2", packageVersions);
        when(componentConfigProvider.getImage(anyLong())).thenReturn(imageInComponent);
        when(imageCatalogService.getImage(anyString(), anyString(), anyString())).thenReturn(statedImage);
        CloudbreakDetails cloudbreakDetails = new CloudbreakDetails();
        cloudbreakDetails.setVersion(StackImageUpdateService.MIN_VERSION);
        when(componentConfigProvider.getCloudbreakDetails(stack.getId())).thenReturn(cloudbreakDetails);
        boolean validImage = underTest.isValidImage(stack, "imageId", "imageCatalogName", "imageCatalogUrl");
        assertTrue(validImage);
    }

    @Test
    public void testIsValidImageFalse() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent =
                new com.sequenceiq.cloudbreak.cloud.model.Image("imageOldName", Collections.emptyMap(), "centos6", "centos",
                        statedImage.getImageCatalogUrl(), statedImage.getImageCatalogName(), "uuid2", packageVersions);
        when(componentConfigProvider.getImage(anyLong())).thenReturn(imageInComponent);
        when(imageCatalogService.getImage(anyString(), anyString(), anyString())).thenReturn(statedImage);
        CloudbreakDetails cloudbreakDetails = new CloudbreakDetails();
        cloudbreakDetails.setVersion("2.4.0");
        when(componentConfigProvider.getCloudbreakDetails(stack.getId())).thenReturn(cloudbreakDetails);
        boolean validImage = underTest.isValidImage(stack, "imageId", "imageCatalogName", "imageCatalogUrl");
        assertFalse(validImage);
    }
}