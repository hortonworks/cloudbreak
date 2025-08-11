package com.sequenceiq.cloudbreak.reactor.handler;

import static com.sequenceiq.common.model.ImageCatalogPlatform.imageCatalogPlatform;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureImageFormatValidator;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.image.userdata.UserDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.model.ImageCatalogPlatform;

@ExtendWith(MockitoExtension.class)
class ImageFallbackServiceTest {

    private static final Long STACK_ID = 123L;

    private static final String USER_CRN = "crn:altus:iam:us-west-1:123:user:456";

    private static final String STACK_CRN = "crn:cdp:datahub:us-west-1:datahub:cluster:f7563fc1-e8ff-486a-9260-4e54ccabbaa0";

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private ImageService imageService;

    @Mock
    private UserDataService userDataService;

    @Mock
    private PlatformStringTransformer platformStringTransformer;

    @Mock
    private AzureImageFormatValidator azureImageFormatValidator;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private StackView stackView;

    @InjectMocks
    private ImageFallbackService imageFallbackService;

    @BeforeEach
    public void setup() {
        when(stackView.getResourceCrn()).thenReturn(STACK_CRN);
    }

    @Test
    public void testFallbackToVhdForNonAzurePlatform() throws Exception {
        com.sequenceiq.cloudbreak.domain.stack.Component component = mock(com.sequenceiq.cloudbreak.domain.stack.Component.class);
        Image currentImage =
                new Image("originalImage", Map.of(), "redhat8", "redhat8", "arch", null, null, null, Map.of(), null, 0L, null);
                new Image("originalImage", Map.of(), "redhat8", "redhat8", "arch", null, null, null, Map.of(), null, 0L, null);

        when(stackDtoService.getStackViewById(STACK_ID)).thenReturn(stackView);
        when(stackView.getCloudPlatform()).thenReturn("AWS");
        when(componentConfigProviderService.getImageComponent(STACK_ID)).thenReturn(component);
        when(component.getAttributes()).thenReturn(new Json(currentImage));

            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
                try {
                    imageFallbackService.fallbackToVhd(STACK_ID);
                } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException | IOException e) {
                    throw new RuntimeException(e);
                }
            });

        verify(stackDtoService).getStackViewById(STACK_ID);
        verifyNoMoreInteractions(stackDtoService);
    }

    @Test
    public void testFallbackToVhdWithMarketplaceOnlyEntitlement() throws Exception {
        com.sequenceiq.cloudbreak.domain.stack.Component component = mock(com.sequenceiq.cloudbreak.domain.stack.Component.class);
        Image currentImage =
                new Image("originalImage", Map.of(), "redhat8", "redhat8", "arch", null, null, null, Map.of(), null, 0L, null);

        when(stackDtoService.getStackViewById(STACK_ID)).thenReturn(stackView);
        when(stackView.getCloudPlatform()).thenReturn("AZURE");
        when(componentConfigProviderService.getImageComponent(STACK_ID)).thenReturn(component);
        when(component.getAttributes()).thenReturn(new Json(currentImage));
        when(entitlementService.azureOnlyMarketplaceImagesEnabled(any())).thenReturn(true);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                imageFallbackService.fallbackToVhd(STACK_ID);
            } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException | IOException e) {
                throw new RuntimeException(e);
            }
        });

        verify(stackDtoService).getStackViewById(STACK_ID);
        verifyNoMoreInteractions(stackDtoService);
    }

    @Test
    public void testFallbackToVhdShouldSetFallbackImage() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException, IOException {
        Image currentImage =
                new Image("originalImage", Map.of(), "centos7", "redhat", "arch", null, null, null, Map.of(), null, 0L, null);

        com.sequenceiq.cloudbreak.domain.stack.Component component  =
                new Component(ComponentType.IMAGE, ComponentType.IMAGE.name(), new Json(currentImage), null);

        when(stackDtoService.getStackViewById(STACK_ID)).thenReturn(stackView);
        when(stackView.getCloudPlatform()).thenReturn("AZURE");
        when(componentConfigProviderService.getImageComponent(anyLong())).thenReturn(component);
        StatedImage statedImage = mock(StatedImage.class);
        when(imageCatalogService.getImage(anyLong(), any(), any(), any())).thenReturn(statedImage);
        when(azureImageFormatValidator.isMarketplaceImageFormat(anyString())).thenReturn(true);
        when(imageService.determineImageNameByRegion(eq("AZURE"), any(), any(), any())).thenReturn("aNewImage");

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                imageFallbackService.fallbackToVhd(STACK_ID);
            } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException | IOException e) {
                throw new RuntimeException(e);
            }
        });
        verify(userDataService).makeSureUserDataIsMigrated(any());
        verify(imageService).determineImageNameByRegion(eq("AZURE"), any(), any(), any());
        ArgumentCaptor<Component> imageCaptor = ArgumentCaptor.forClass(Component.class);
        verify(componentConfigProviderService).store(imageCaptor.capture());
        assertEquals("aNewImage", imageCaptor.getValue().getAttributes().get(Image.class).getImageName());
    }

    @Test
    public void testGetFallbackImageNameShouldReturnImageName() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        StatedImage statedImage = mock(StatedImage.class);
        Image image = mock(Image.class);

        when(image.getImageName()).thenReturn("marketplaceImage");
        when(azureImageFormatValidator.isMarketplaceImageFormat(anyString())).thenReturn(true);
        when(imageCatalogService.getImage(anyLong(), any(), any(), any())).thenReturn(statedImage);
        when(imageService.determineImageNameByRegion(any(), any(), any(), any())).thenReturn("aNewImage");
        when(stackView.getCloudPlatform()).thenReturn("AZURE");
        when(stackView.getPlatformVariant()).thenReturn("AZURE");
        when(stackView.getRegion()).thenReturn("region");
        ImageCatalogPlatform azure = imageCatalogPlatform("AZURE");
        when(platformStringTransformer.getPlatformStringForImageCatalog(anyString(), anyString())).thenReturn(azure);

        String result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return imageFallbackService.getFallbackImageName(stackView, image);
            } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
                throw new RuntimeException(e);
            }
        });

        assertNotNull(result);
        verify(imageService).determineImageNameByRegion(eq("AZURE"), eq(azure), eq("region"), any());
    }

    @Test
    public void testGetFallbackImageNameShouldNotFallbackIfNoImageName() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        StatedImage statedImage = mock(StatedImage.class);
        Image image = mock(Image.class);

        Image currentImage =
                new Image("originalImage", Map.of(), "centos7", "redhat", "arch", null, null, null, Map.of(), null, 0L, null);
        com.sequenceiq.cloudbreak.domain.stack.Component component  =
                new Component(ComponentType.IMAGE, ComponentType.IMAGE.name(), new Json(currentImage), null);

        when(stackDtoService.getStackViewById(STACK_ID)).thenReturn(stackView);
        when(azureImageFormatValidator.isMarketplaceImageFormat(anyString())).thenReturn(true);
        when(componentConfigProviderService.getImageComponent(anyLong())).thenReturn(component);
        doThrow(new CloudbreakImageNotFoundException("Image not found")).when(imageCatalogService).getImage(anyLong(), any(), any(), any());
        when(stackView.getCloudPlatform()).thenReturn("AZURE");
        when(stackView.getPlatformVariant()).thenReturn("AZURE");
        ImageCatalogPlatform azure = imageCatalogPlatform("AZURE");
        when(platformStringTransformer.getPlatformStringForImageCatalog(anyString(), anyString())).thenReturn(azure);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                imageFallbackService.fallbackToVhd(STACK_ID);
            } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException | IOException e) {
                throw new RuntimeException(e);
            }
        }));

        assertEquals("Your image originalImage seems to be an Azure Marketplace image, however its Terms and Conditions are not accepted! " +
                "Please either enable automatic consent or accept the terms manually and initiate the provisioning or upgrade again. " +
                "On how to accept the Terms and Conditions of the image please refer to azure documentation at " +
                "https://docs.microsoft.com/en-us/cli/azure/vm/image/terms?view=azure-cli-latest.", exception.getMessage());
    }

    @Test
    public void testGetFallbackImageNameShouldNotRunForOtherCloudProvider() {
        Image image = mock(Image.class);

        when(image.getImageName()).thenReturn("marketplaceImage");
        when(stackView.getCloudPlatform()).thenReturn("AWS");

        String result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return imageFallbackService.getFallbackImageName(stackView, image);
            } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
                throw new RuntimeException(e);
            }
        });

        assertNull(result);
        verify(azureImageFormatValidator).isMarketplaceImageFormat(anyString());
        verify(entitlementService).azureOnlyMarketplaceImagesEnabled(anyString());
        verifyNoMoreInteractions(azureImageFormatValidator);
    }

    @Test
    public void testGetFallbackImageNameShouldNotRunForForVhdImage() {
        Image image = mock(Image.class);

        when(image.getImageName()).thenReturn("vhdImage");
        when(azureImageFormatValidator.isMarketplaceImageFormat(anyString())).thenReturn(false);
        when(stackView.getCloudPlatform()).thenReturn("AZURE");

        String result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return imageFallbackService.getFallbackImageName(stackView, image);
            } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
                throw new RuntimeException(e);
            }
        });

        assertNull(result);
        verify(azureImageFormatValidator).isMarketplaceImageFormat("vhdImage");
    }

    @Test
    public void testGetFallbackImageNameShouldReturnNullIfException() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Image image = mock(Image.class);

        when(image.getImageName()).thenReturn("anImage");
        when(azureImageFormatValidator.isMarketplaceImageFormat(anyString())).thenReturn(true);
        when(stackView.getCloudPlatform()).thenReturn("AZURE");
        when(imageService.determineImageNameByRegion(any(), any(), any(), any())).
                thenThrow(new CloudbreakImageNotFoundException("Image not found"));
        StatedImage statedImage = mock(StatedImage.class);
        when(imageCatalogService.getImage(anyLong(), any(), any(), any())).thenReturn(statedImage);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                imageFallbackService.getFallbackImageName(stackView, image);
            } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
                throw new RuntimeException(e);
            }
        }));

        assertEquals("com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException: Image not found", exception.getMessage());
        verify(azureImageFormatValidator).isMarketplaceImageFormat("anImage");
    }
}