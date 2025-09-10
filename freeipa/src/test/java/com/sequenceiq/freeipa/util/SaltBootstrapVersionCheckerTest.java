package com.sequenceiq.freeipa.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.dto.ImageWrapper;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.image.FreeIpaImageFilterSettings;
import com.sequenceiq.freeipa.service.image.ImageService;

@ExtendWith(MockitoExtension.class)
class SaltBootstrapVersionCheckerTest {

    @Mock
    private ImageService imageService;

    @Mock
    private Stack stack;

    @Mock
    private Image image;

    @InjectMocks
    private SaltBootstrapVersionChecker underTest;

    @BeforeEach
    void setUp() {
        when(imageService.getImageForStack(stack)).thenReturn(image);
    }

    @Test
    void isChangeSaltuserPasswordSupportedTrue() {
        setSaltBootstrapVersion("0.13.6-2022-05-31T16:13:05");

        boolean result = underTest.isChangeSaltuserPasswordSupported(stack);

        assertTrue(result);
    }

    @Test
    void isChangeSaltuserPasswordSupportedFalse() {
        setSaltBootstrapVersion("0.13.5-2022-05-31T16:13:05");

        boolean result = underTest.isChangeSaltuserPasswordSupported(stack);

        assertFalse(result);
    }

    @Test
    void isChangeSaltuserPasswordSupportedFalseWithoutPackageVersion() {
        boolean result = underTest.isChangeSaltuserPasswordSupported(stack);

        assertFalse(result);
    }

    @Test
    void isChangeSaltuserPasswordSupportedFalseWithoutImage() {
        when(imageService.getImageForStack(stack)).thenReturn(null);

        boolean result = underTest.isChangeSaltuserPasswordSupported(stack);

        assertFalse(result);
    }

    private void setSaltBootstrapVersion(String version) {
        when(image.getPackageVersions()).thenReturn(Map.of("salt-bootstrap", version));
    }

    @Test
    void isFqdnAsHostnameSupportedTrueWithMinVersion() {
        setSaltBootstrapVersion("0.14.4-2025-07-02T16:29:51");
        when(image.getUuid()).thenReturn("some-id");
        FreeIpaImageFilterSettings imageFilterSettings = mock(FreeIpaImageFilterSettings.class);
        when(imageService.createImageFilterSettingsFromImageEntity(stack)).thenReturn(imageFilterSettings);
        when(imageFilterSettings.withImageId("different-image-id")).thenReturn(imageFilterSettings);

        InstanceMetaData im1 = new InstanceMetaData();
        com.sequenceiq.cloudbreak.cloud.model.Image instanceImage = mock(com.sequenceiq.cloudbreak.cloud.model.Image.class);
        when(instanceImage.getImageId()).thenReturn("different-image-id");
        Json imageJson = mock(Json.class);
        when(imageJson.getUnchecked(com.sequenceiq.cloudbreak.cloud.model.Image.class)).thenReturn(instanceImage);
        im1.setImage(imageJson);


        InstanceMetaData im2 = new InstanceMetaData();
        com.sequenceiq.cloudbreak.cloud.model.Image instanceImage2 = mock(com.sequenceiq.cloudbreak.cloud.model.Image.class);
        when(instanceImage2.getImageId()).thenReturn("some-id");
        Json imageJson2 = mock(Json.class);
        when(imageJson2.getUnchecked(com.sequenceiq.cloudbreak.cloud.model.Image.class)).thenReturn(instanceImage2);
        im2.setImage(imageJson2);

        when(imageService.getImage(imageFilterSettings)).thenReturn(ImageWrapper.ofFreeipaImage(image, ""));
        when(stack.getNotTerminatedInstanceMetaDataSet()).thenReturn(Set.of(im1, im2));

        boolean result = underTest.isFqdnAsHostnameSupported(stack);

        assertTrue(result);
    }

    @Test
    void isFqdnAsHostnameSupportedFalseWithLowerVersion() {
        setSaltBootstrapVersion("0.14.3-2025-07-02T16:29:51");

        boolean result = underTest.isFqdnAsHostnameSupported(stack);

        assertFalse(result);
    }

    @Test
    void isFqdnAsHostnameSupportedFalseWithoutImage() {
        when(imageService.getImageForStack(stack)).thenReturn(null);

        boolean result = underTest.isFqdnAsHostnameSupported(stack);

        assertFalse(result);
    }
}
