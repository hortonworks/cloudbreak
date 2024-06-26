package com.sequenceiq.cloudbreak.service.image;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

@ExtendWith(MockitoExtension.class)
public class CurrentImageUsageConditionTest {

    private static final long STACK_ID = 1L;

    private static final String NEW_IMAGE = "new-image";

    private static final String OLD_IMAGE = "old-image";

    @InjectMocks
    private CurrentImageUsageCondition underTest;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Spy
    private ImageConverter imageConverter;

    @Test
    public void testFilterCurrentImageShouldReturnTrueWhenThereAreInstanceWithOtherImage() {
        Set<InstanceMetaData> instanceMetaData = createInstanceMetaData(NEW_IMAGE, OLD_IMAGE);
        when(instanceMetaDataService.getNotDeletedAndNotZombieInstanceMetadataByStackId(STACK_ID)).thenReturn(instanceMetaData);

        assertFalse(underTest.currentImageUsedOnInstances(STACK_ID, NEW_IMAGE));

        verify(instanceMetaDataService).getNotDeletedAndNotZombieInstanceMetadataByStackId(STACK_ID);
    }

    @Test
    public void testFilterCurrentImageShouldReturnTrueWhenThereAreNoInstanceWithOtherImage() {
        Set<InstanceMetaData> instanceMetaData = createInstanceMetaData(NEW_IMAGE, NEW_IMAGE);
        when(instanceMetaDataService.getNotDeletedAndNotZombieInstanceMetadataByStackId(STACK_ID)).thenReturn(instanceMetaData);

        assertTrue(underTest.currentImageUsedOnInstances(STACK_ID, NEW_IMAGE));

        verify(instanceMetaDataService).getNotDeletedAndNotZombieInstanceMetadataByStackId(STACK_ID);
    }

    @Test
    public void testFilterCurrentImageShouldReturnFalseWhenOneOfTheImageJsonIsNull() {
        InstanceMetaData nullImageInstanceMetadata = new InstanceMetaData();
        nullImageInstanceMetadata.setImage(new Json(null));
        Set<InstanceMetaData> instanceMetaData = createInstanceMetaData(NEW_IMAGE);
        instanceMetaData.add(nullImageInstanceMetadata);
        when(instanceMetaDataService.getNotDeletedAndNotZombieInstanceMetadataByStackId(STACK_ID)).thenReturn(instanceMetaData);

        assertTrue(underTest.currentImageUsedOnInstances(STACK_ID, NEW_IMAGE));

        verify(instanceMetaDataService).getNotDeletedAndNotZombieInstanceMetadataByStackId(STACK_ID);
    }

    @Test
    public void testFilterCurrentImageShouldReturnTrueWhenOneOfTheImageJsonIsNullButTheOtherIsOnOldImage() {
        InstanceMetaData nullImageInstanceMetadata = new InstanceMetaData();
        nullImageInstanceMetadata.setImage(new Json(null));
        Set<InstanceMetaData> instanceMetaData = createInstanceMetaData(OLD_IMAGE);
        instanceMetaData.add(nullImageInstanceMetadata);
        when(instanceMetaDataService.getNotDeletedAndNotZombieInstanceMetadataByStackId(STACK_ID)).thenReturn(instanceMetaData);

        assertFalse(underTest.currentImageUsedOnInstances(STACK_ID, NEW_IMAGE));

        verify(instanceMetaDataService).getNotDeletedAndNotZombieInstanceMetadataByStackId(STACK_ID);
    }

    @Test
    public void testFilterCurrentImageShouldReturnFalseWhenBothOfTheImageJsonIsNull() {
        InstanceMetaData nullImageInstanceMetadata = new InstanceMetaData();
        nullImageInstanceMetadata.setImage(new Json(null));
        InstanceMetaData nullImageInstanceMetadata2 = new InstanceMetaData();
        nullImageInstanceMetadata2.setImage(new Json(null));
        Set<InstanceMetaData> instanceMetaData = Set.of(nullImageInstanceMetadata, nullImageInstanceMetadata2);
        when(instanceMetaDataService.getNotDeletedAndNotZombieInstanceMetadataByStackId(STACK_ID)).thenReturn(instanceMetaData);

        assertFalse(underTest.currentImageUsedOnInstances(STACK_ID, NEW_IMAGE));

        verify(instanceMetaDataService).getNotDeletedAndNotZombieInstanceMetadataByStackId(STACK_ID);
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image createImage(String imageId) {
        return Image.builder().withImageId(imageId).build();
    }

    private Set<InstanceMetaData> createInstanceMetaData(String... imageId) {
        Set<InstanceMetaData> instanceMetaDataSet = new HashSet<>();
        for (int i = 0; i < imageId.length; i++) {
            String s = imageId[i];
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setInstanceId("instance-" + i);
            instanceMetaData.setImage(new Json(createImage(imageId[i])));
            instanceMetaDataSet.add(instanceMetaData);
        }
        return instanceMetaDataSet;
    }

}