package com.sequenceiq.freeipa.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.freeipa.dto.ImageWrapper;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.image.FreeIpaImageFilterSettings;
import com.sequenceiq.freeipa.service.image.ImageNotFoundException;
import com.sequenceiq.freeipa.service.image.ImageService;

@ExtendWith(MockitoExtension.class)
class LdapAgentAvailabilityCheckerTest {

    private static final String LDAP_PACKAGE_NAME = "freeipa-ldap-agent";

    private static final String CURRENT_IMAGE_ID = "current-image-id";

    private static final String INSTANCE_IMAGE_ID = "instance-image-id";

    @Mock
    private ImageService imageService;

    @InjectMocks
    private LdapAgentAvailabilityChecker underTest;

    @Test
    void testIsLdapAgentTlsSupportAvailableWhenPackageAndImageSupportTls() {
        Stack stack = new Stack();

        com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image image = mock(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image.class);
        when(imageService.getImageForStack(stack)).thenReturn(image);

        Map<String, String> packageVersions = new HashMap<>();
        packageVersions.put(LDAP_PACKAGE_NAME, "1.1.0.3-b525");
        when(image.getPackageVersions()).thenReturn(packageVersions);

        when(image.getUuid()).thenReturn(INSTANCE_IMAGE_ID);

        boolean result = underTest.isLdapAgentTlsSupportAvailable(stack);

        assertTrue(result);
    }

    @Test
    void testIsLdapAgentTlsSupportAvailableWhenPackageDoesNotSupportTls() {
        Stack stack = new Stack();

        com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image image = mock(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image.class);
        when(imageService.getImageForStack(stack)).thenReturn(image);

        Map<String, String> packageVersions = new HashMap<>();
        packageVersions.put(LDAP_PACKAGE_NAME, "1.0.0.0-b525");
        when(image.getPackageVersions()).thenReturn(packageVersions);

        boolean result = underTest.isLdapAgentTlsSupportAvailable(stack);

        assertFalse(result);
    }

    @Test
    void testIsLdapAgentTlsSupportAvailableWhenImageDoesNotSupportTls() {
        Stack stack = new Stack();

        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceMetaData instanceMetaData = new InstanceMetaData();

        Image instanceImage = mock(Image.class);
        when(instanceImage.getImageId()).thenReturn("different-image-id");

        Json imageJson = mock(Json.class);
        when(imageJson.getUnchecked(Image.class)).thenReturn(instanceImage);
        instanceMetaData.setImage(imageJson);

        Set<InstanceMetaData> instanceMetaDataSet = Set.of(instanceMetaData);
        instanceGroup.setInstanceMetaData(instanceMetaDataSet);

        stack.setInstanceGroups(Set.of(instanceGroup));

        com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image currentImage = mock(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image.class);
        when(currentImage.getUuid()).thenReturn(CURRENT_IMAGE_ID);
        when(imageService.getImageForStack(stack)).thenReturn(currentImage);

        Map<String, String> packageVersions = new HashMap<>();
        packageVersions.put(LDAP_PACKAGE_NAME, "1.1.0.4-b525");
        when(currentImage.getPackageVersions()).thenReturn(packageVersions);

        FreeIpaImageFilterSettings imageFilterSettings = mock(FreeIpaImageFilterSettings.class);
        when(imageService.createImageFilterSettingsFromImageEntity(stack)).thenReturn(imageFilterSettings);
        when(imageFilterSettings.withImageId(any())).thenReturn(imageFilterSettings);

        when(imageService.getImage(imageFilterSettings)).thenThrow(new ImageNotFoundException("Image not found"));

        boolean result = underTest.isLdapAgentTlsSupportAvailable(stack);

        assertFalse(result);
    }

    @Test
    void testDoesAllImageSupportTlsWhenAllImagesAreSame() {
        Stack stack = new Stack();

        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceMetaData instanceMetaData = new InstanceMetaData();

        Image instanceImage = mock(Image.class);
        when(instanceImage.getImageId()).thenReturn(INSTANCE_IMAGE_ID);

        Json imageJson = mock(Json.class);
        when(imageJson.getUnchecked(Image.class)).thenReturn(instanceImage);
        instanceMetaData.setImage(imageJson);

        Set<InstanceMetaData> instanceMetaDataSet = Set.of(instanceMetaData);
        instanceGroup.setInstanceMetaData(instanceMetaDataSet);

        stack.setInstanceGroups(Set.of(instanceGroup));

        com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image currentImage = mock(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image.class);
        when(currentImage.getUuid()).thenReturn(INSTANCE_IMAGE_ID);
        when(imageService.getImageForStack(stack)).thenReturn(currentImage);

        Map<String, String> packageVersions = new HashMap<>();
        packageVersions.put(LDAP_PACKAGE_NAME, "1.1.0.4-b525");
        when(currentImage.getPackageVersions()).thenReturn(packageVersions);

        boolean result = underTest.isLdapAgentTlsSupportAvailable(stack);

        assertTrue(result);
    }

    @Test
    void testDoesAllImageSupportTlsWhenNonCurrentImagesSupportTls() {
        Stack stack = new Stack();

        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceMetaData instanceMetaData = new InstanceMetaData();

        Image instanceImage = mock(Image.class);
        when(instanceImage.getImageId()).thenReturn("different-image-id");

        Json imageJson = mock(Json.class);
        when(imageJson.getUnchecked(Image.class)).thenReturn(instanceImage);
        instanceMetaData.setImage(imageJson);

        Set<InstanceMetaData> instanceMetaDataSet = Set.of(instanceMetaData);
        instanceGroup.setInstanceMetaData(instanceMetaDataSet);

        stack.setInstanceGroups(Set.of(instanceGroup));

        com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image currentImage = mock(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image.class);
        when(currentImage.getUuid()).thenReturn(CURRENT_IMAGE_ID);
        when(imageService.getImageForStack(stack)).thenReturn(currentImage);

        Map<String, String> currentPackageVersions = new HashMap<>();
        currentPackageVersions.put(LDAP_PACKAGE_NAME, "1.1.0.4-b525");
        when(currentImage.getPackageVersions()).thenReturn(currentPackageVersions);

        FreeIpaImageFilterSettings imageFilterSettings = mock(FreeIpaImageFilterSettings.class);
        when(imageService.createImageFilterSettingsFromImageEntity(stack)).thenReturn(imageFilterSettings);
        when(imageFilterSettings.withImageId(any())).thenReturn(imageFilterSettings);

        ImageWrapper imageWrapper = mock(ImageWrapper.class);
        when(imageService.getImage(imageFilterSettings)).thenReturn(imageWrapper);

        com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image nonCurrentImage =
                mock(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image.class);
        when(imageWrapper.getImage()).thenReturn(nonCurrentImage);

        Map<String, String> nonCurrentPackageVersions = new HashMap<>();
        nonCurrentPackageVersions.put(LDAP_PACKAGE_NAME, "1.1.0.4-b525");
        when(nonCurrentImage.getPackageVersions()).thenReturn(nonCurrentPackageVersions);

        boolean result = underTest.isLdapAgentTlsSupportAvailable(stack);

        assertTrue(result);
    }

    @Test
    void testDoesAllImageSupportTlsWhenNonCurrentImagesDoNotSupportTls() {
        Stack stack = new Stack();

        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceMetaData instanceMetaData = new InstanceMetaData();

        Image instanceImage = mock(Image.class);
        when(instanceImage.getImageId()).thenReturn("different-image-id");

        Json imageJson = mock(Json.class);
        when(imageJson.getUnchecked(Image.class)).thenReturn(instanceImage);
        instanceMetaData.setImage(imageJson);

        Set<InstanceMetaData> instanceMetaDataSet = Set.of(instanceMetaData);
        instanceGroup.setInstanceMetaData(instanceMetaDataSet);

        stack.setInstanceGroups(Set.of(instanceGroup));

        com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image currentImage = mock(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image.class);
        when(currentImage.getUuid()).thenReturn(CURRENT_IMAGE_ID);
        when(imageService.getImageForStack(stack)).thenReturn(currentImage);

        Map<String, String> currentPackageVersions = new HashMap<>();
        currentPackageVersions.put(LDAP_PACKAGE_NAME, "1.1.0.4-b525");
        when(currentImage.getPackageVersions()).thenReturn(currentPackageVersions);

        FreeIpaImageFilterSettings imageFilterSettings = mock(FreeIpaImageFilterSettings.class);
        when(imageService.createImageFilterSettingsFromImageEntity(stack)).thenReturn(imageFilterSettings);
        when(imageFilterSettings.withImageId(any())).thenReturn(imageFilterSettings);

        ImageWrapper imageWrapper = mock(ImageWrapper.class);
        when(imageService.getImage(imageFilterSettings)).thenReturn(imageWrapper);

        com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image nonCurrentImage =
                mock(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image.class);
        when(imageWrapper.getImage()).thenReturn(nonCurrentImage);

        Map<String, String> nonCurrentPackageVersions = new HashMap<>();
        nonCurrentPackageVersions.put(LDAP_PACKAGE_NAME, "1.0.0.0-b525");
        when(nonCurrentImage.getPackageVersions()).thenReturn(nonCurrentPackageVersions);

        boolean result = underTest.isLdapAgentTlsSupportAvailable(stack);

        assertFalse(result);
    }

    @Test
    void testDoesAllNonCurrentImageSupportTlsWhenImageNotFound() {
        Stack stack = new Stack();

        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceMetaData instanceMetaData = new InstanceMetaData();

        Image instanceImage = mock(Image.class);
        when(instanceImage.getImageId()).thenReturn("different-image-id");

        Json imageJson = mock(Json.class);
        when(imageJson.getUnchecked(Image.class)).thenReturn(instanceImage);
        instanceMetaData.setImage(imageJson);

        Set<InstanceMetaData> instanceMetaDataSet = Set.of(instanceMetaData);
        instanceGroup.setInstanceMetaData(instanceMetaDataSet);

        stack.setInstanceGroups(Set.of(instanceGroup));

        com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image currentImage =
                mock(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image.class);
        when(currentImage.getUuid()).thenReturn(CURRENT_IMAGE_ID);
        when(imageService.getImageForStack(stack)).thenReturn(currentImage);

        Map<String, String> currentPackageVersions = new HashMap<>();
        currentPackageVersions.put(LDAP_PACKAGE_NAME, "1.1.0.4-b525");
        when(currentImage.getPackageVersions()).thenReturn(currentPackageVersions);

        FreeIpaImageFilterSettings imageFilterSettings = mock(FreeIpaImageFilterSettings.class);
        when(imageService.createImageFilterSettingsFromImageEntity(stack)).thenReturn(imageFilterSettings);
        when(imageFilterSettings.withImageId(any())).thenReturn(imageFilterSettings);

        when(imageService.getImage(imageFilterSettings)).thenThrow(new ImageNotFoundException("Image not found"));

        boolean result = underTest.isLdapAgentTlsSupportAvailable(stack);

        assertFalse(result);
    }

}
