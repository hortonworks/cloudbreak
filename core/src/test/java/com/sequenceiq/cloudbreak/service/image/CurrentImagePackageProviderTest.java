package com.sequenceiq.cloudbreak.service.image;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

@ExtendWith(MockitoExtension.class)
class CurrentImagePackageProviderTest {

    private static final long STACK_ID = 1L;

    private static final ImagePackageVersion REQUIRED_PACKAGE = ImagePackageVersion.PYTHON38;

    private static final String CURRENT_IMAGE_ID = "current-image";

    @InjectMocks
    private CurrentImagePackageProvider underTest;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Spy
    private ImageConverter imageConverter;

    @Test
    void testShouldReturnTrueWhenAllInstanceContainsTheRequiredPackage() {
        when(instanceMetaDataService.getNotDeletedAndNotZombieInstanceMetadataByStackId(STACK_ID)).thenReturn(createInstanceMetadataSet());
        List<Image> cdhImagesFromCatalog = List.of(createImage("image-1", true), createImage("image-2", true), createImage(CURRENT_IMAGE_ID, true));
        assertTrue(underTest.currentInstancesContainsPackage(STACK_ID, cdhImagesFromCatalog, REQUIRED_PACKAGE));
    }

    @Test
    void testShouldReturnFalseWhenInstancesDoesNotContainsTheRequiredPackage() {
        when(instanceMetaDataService.getNotDeletedAndNotZombieInstanceMetadataByStackId(STACK_ID)).thenReturn(createInstanceMetadataSet());
        List<Image> cdhImagesFromCatalog = List.of(createImage("image-1", true), createImage("image-2", false), createImage(CURRENT_IMAGE_ID, false));
        assertFalse(underTest.currentInstancesContainsPackage(STACK_ID, cdhImagesFromCatalog, REQUIRED_PACKAGE));
    }

    @Test
    void testShouldReturnFalseWhenTheImagesFromCatalogIsEmpty() {
        when(instanceMetaDataService.getNotDeletedAndNotZombieInstanceMetadataByStackId(STACK_ID)).thenReturn(createInstanceMetadataSet());
        List<Image> cdhImagesFromCatalog = Collections.emptyList();
        assertFalse(underTest.currentInstancesContainsPackage(STACK_ID, cdhImagesFromCatalog, REQUIRED_PACKAGE));
    }

    private Image createImage(String imageId, boolean containsPackage) {
        return Image.builder()
                .withUuid(imageId)
                .withPackageVersions(containsPackage ? Map.of(REQUIRED_PACKAGE.getKey(), "3.8") : Collections.emptyMap())
                .build();
    }

    private Set<InstanceMetaData> createInstanceMetadataSet() {
        return Set.of(
                createInstanceMetadata("instance-1", CURRENT_IMAGE_ID),
                createInstanceMetadata("instance-2", CURRENT_IMAGE_ID),
                createInstanceMetadata("instance-3", CURRENT_IMAGE_ID)
        );
    }

    private InstanceMetaData createInstanceMetadata(String instanceId, String imageId) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId(instanceId);
        instanceMetaData.setImage(new Json(createImage(imageId)));
        return instanceMetaData;
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image createImage(String imageId) {
        return com.sequenceiq.cloudbreak.cloud.model.Image.builder().withImageId(imageId).build();
    }
}