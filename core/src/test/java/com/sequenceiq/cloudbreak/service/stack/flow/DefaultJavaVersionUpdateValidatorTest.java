package com.sequenceiq.cloudbreak.service.stack.flow;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.SetDefaultJavaVersionRequest;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.java.vm.AllowableJavaUpdateConfigurations;

@ExtendWith(MockitoExtension.class)
class DefaultJavaVersionUpdateValidatorTest {

    private static final long STACK_ID = 1L;

    private static final String STACK_NAME = "stackName";

    @Mock
    private StackDto stack;

    @Mock
    private ImageService imageService;

    @Mock
    private AllowableJavaUpdateConfigurations allowableJavaUpdateConfigurations;

    @InjectMocks
    private DefaultJavaVersionUpdateValidator underTest;

    @BeforeEach
    void setUp() {
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getName()).thenReturn(STACK_NAME);
    }

    @Test
    void testValidateWhenImageCouldNotBeFound() throws CloudbreakImageNotFoundException {
        when(imageService.getImage(STACK_ID)).thenThrow(new CloudbreakImageNotFoundException("Not found"));
        SetDefaultJavaVersionRequest javaVersionRequest = new SetDefaultJavaVersionRequest();
        javaVersionRequest.setDefaultJavaVersion("8");

        BadRequestException actual = assertThrows(BadRequestException.class, () -> underTest.validate(stack, javaVersionRequest));

        assertEquals("Image information could not be found for the cluster with name 'stackName'", actual.getMessage());
    }

    @Test
    void testValidateWhenImageDoesNotContainTheRequestedJavaVersion() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Image mockImage = mock(Image.class);
        when(mockImage.getImageId()).thenReturn("mockImageId");
        Map<String, String> imagePackageVersions = Map.of(
                "java", "8",
                "java11", "11.0.23",
                "java8", "1.8.0_412");
        when(mockImage.getPackageVersions()).thenReturn(imagePackageVersions);
        when(imageService.getImage(STACK_ID)).thenReturn(mockImage);
        SetDefaultJavaVersionRequest javaVersionRequest = new SetDefaultJavaVersionRequest();
        javaVersionRequest.setDefaultJavaVersion("21");

        BadRequestException actual = assertThrows(BadRequestException.class, () -> underTest.validate(stack, javaVersionRequest));

        assertEquals("The requested Java version '21' could not be found on the VM image('mockImageId') of the cluster.", actual.getMessage());
    }

    @Test
    void testValidateWhenImageContainsTheRequestedJavaVersion() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Image mockImage = mock(Image.class);
        when(mockImage.getImageId()).thenReturn("mockImageId");
        Map<String, String> imagePackageVersions = Map.of(
                "java", "8",
                "java11", "11.0.23",
                "java8", "1.8.0_412",
                ImagePackageVersion.STACK.getKey(), "7.3.1");
        when(mockImage.getPackageVersions()).thenReturn(imagePackageVersions);
        when(imageService.getImage(STACK_ID)).thenReturn(mockImage);
        when(imageService.getCurrentImage(anyLong(), eq(STACK_ID)))
                .thenReturn(StatedImage.statedImage(mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class), "url", "catalog"));
        SetDefaultJavaVersionRequest javaVersionRequest = new SetDefaultJavaVersionRequest();
        javaVersionRequest.setDefaultJavaVersion("11");

        assertDoesNotThrow(() -> underTest.validate(stack, javaVersionRequest));
        verify(allowableJavaUpdateConfigurations).checkValidConfiguration(11, "7.3.1");
    }

    @Test
    void testValidateWhenJavaConfigNotAllowed() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Image mockImage = mock(Image.class);
        when(mockImage.getImageId()).thenReturn("mockImageId");
        Map<String, String> imagePackageVersions = Map.of(
                "java", "8",
                "java11", "11.0.23",
                "java8", "1.8.0_412",
                ImagePackageVersion.STACK.getKey(), "7.3.1");
        when(mockImage.getPackageVersions()).thenReturn(imagePackageVersions);
        when(imageService.getImage(STACK_ID)).thenReturn(mockImage);
        when(imageService.getCurrentImage(anyLong(), eq(STACK_ID)))
                .thenReturn(StatedImage.statedImage(mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class), "url", "catalog"));
        SetDefaultJavaVersionRequest javaVersionRequest = new SetDefaultJavaVersionRequest();
        javaVersionRequest.setDefaultJavaVersion("11");
        doThrow(new BadRequestException("Not valid")).when(allowableJavaUpdateConfigurations).checkValidConfiguration(11, "7.3.1");

        BadRequestException actual = assertThrows(BadRequestException.class, () -> underTest.validate(stack, javaVersionRequest));
        assertEquals("Not valid", actual.getMessage());
    }

    @Test
    void testValidateWhenNoRuntimeInfo() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Image mockImage = mock(Image.class);
        String mockImageId = "mockImageId";
        when(mockImage.getImageId()).thenReturn(mockImageId);
        Map<String, String> imagePackageVersions = Map.of(
                "java", "8",
                "java11", "11.0.23",
                "java8", "1.8.0_412");
        when(mockImage.getPackageVersions()).thenReturn(imagePackageVersions);
        when(imageService.getImage(STACK_ID)).thenReturn(mockImage);
        when(imageService.getCurrentImage(anyLong(), eq(STACK_ID)))
                .thenReturn(StatedImage.statedImage(mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class), "url", "catalog"));
        SetDefaultJavaVersionRequest javaVersionRequest = new SetDefaultJavaVersionRequest();
        javaVersionRequest.setDefaultJavaVersion("11");

        assertDoesNotThrow(() -> underTest.validate(stack, javaVersionRequest));
        verifyNoInteractions(allowableJavaUpdateConfigurations);
    }

    @Test
    void testValidateWhenImageContainsTheRequestedJavaVersionAndImageContainsReleaseVersion()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Image mockImage = mock(Image.class);
        when(mockImage.getImageId()).thenReturn("mockImageId");
        when(mockImage.getTags()).thenReturn(Map.of("release-version", "7.3.1.500"));
        Map<String, String> imagePackageVersions = Map.of(
                "java", "8",
                "java11", "11.0.23",
                "java8", "1.8.0_412",
                ImagePackageVersion.STACK.getKey(), "7.3.1");
        when(mockImage.getPackageVersions()).thenReturn(imagePackageVersions);
        when(imageService.getImage(STACK_ID)).thenReturn(mockImage);

        SetDefaultJavaVersionRequest javaVersionRequest = new SetDefaultJavaVersionRequest();
        javaVersionRequest.setDefaultJavaVersion("11");

        assertDoesNotThrow(() -> underTest.validate(stack, javaVersionRequest));
        verify(allowableJavaUpdateConfigurations).checkValidConfiguration(11, "7.3.1.500");
        verify(imageService, never()).getCurrentImage(anyLong(), eq(STACK_ID));
    }
}