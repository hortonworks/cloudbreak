package com.sequenceiq.cloudbreak.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.sequenceiq.common.model.OsType;

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
        Set<InstanceMetaData> instanceMetaData = createInstanceMetaDataWithImage(NEW_IMAGE, OLD_IMAGE);
        when(instanceMetaDataService.getNotDeletedAndNotZombieInstanceMetadataByStackId(STACK_ID)).thenReturn(instanceMetaData);

        assertFalse(underTest.isCurrentImageUsedOnInstances(STACK_ID, NEW_IMAGE));

        verify(instanceMetaDataService).getNotDeletedAndNotZombieInstanceMetadataByStackId(STACK_ID);
    }

    @Test
    public void testFilterCurrentImageShouldReturnTrueWhenThereAreNoInstanceWithOtherImage() {
        Set<InstanceMetaData> instanceMetaData = createInstanceMetaDataWithImage(NEW_IMAGE, NEW_IMAGE);
        when(instanceMetaDataService.getNotDeletedAndNotZombieInstanceMetadataByStackId(STACK_ID)).thenReturn(instanceMetaData);

        assertTrue(underTest.isCurrentImageUsedOnInstances(STACK_ID, NEW_IMAGE));

        verify(instanceMetaDataService).getNotDeletedAndNotZombieInstanceMetadataByStackId(STACK_ID);
    }

    @Test
    public void testFilterCurrentImageShouldReturnFalseWhenOneOfTheImageJsonIsNull() {
        InstanceMetaData nullImageInstanceMetadata = new InstanceMetaData();
        nullImageInstanceMetadata.setImage(new Json(null));
        Set<InstanceMetaData> instanceMetaData = createInstanceMetaDataWithImage(NEW_IMAGE);
        instanceMetaData.add(nullImageInstanceMetadata);
        when(instanceMetaDataService.getNotDeletedAndNotZombieInstanceMetadataByStackId(STACK_ID)).thenReturn(instanceMetaData);

        assertTrue(underTest.isCurrentImageUsedOnInstances(STACK_ID, NEW_IMAGE));

        verify(instanceMetaDataService).getNotDeletedAndNotZombieInstanceMetadataByStackId(STACK_ID);
    }

    @Test
    public void testFilterCurrentImageShouldReturnTrueWhenOneOfTheImageJsonIsNullButTheOtherIsOnOldImage() {
        InstanceMetaData nullImageInstanceMetadata = new InstanceMetaData();
        nullImageInstanceMetadata.setImage(new Json(null));
        Set<InstanceMetaData> instanceMetaData = createInstanceMetaDataWithImage(OLD_IMAGE);
        instanceMetaData.add(nullImageInstanceMetadata);
        when(instanceMetaDataService.getNotDeletedAndNotZombieInstanceMetadataByStackId(STACK_ID)).thenReturn(instanceMetaData);

        assertFalse(underTest.isCurrentImageUsedOnInstances(STACK_ID, NEW_IMAGE));

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

        assertFalse(underTest.isCurrentImageUsedOnInstances(STACK_ID, NEW_IMAGE));

        verify(instanceMetaDataService).getNotDeletedAndNotZombieInstanceMetadataByStackId(STACK_ID);
    }

    @Test
    public void testIsCurrentOsUsedOnInstancesShouldReturnTrueWhenAllInstanceUsingTheSameOS() {
        Set<InstanceMetaData> instanceMetaData = createInstanceMetaDataWithOs(OsType.CENTOS7, OsType.CENTOS7, OsType.CENTOS7);
        when(instanceMetaDataService.getNotDeletedAndNotZombieInstanceMetadataByStackId(STACK_ID)).thenReturn(instanceMetaData);

        assertEquals(Set.of(OsType.CENTOS7), underTest.getOSUsedByInstances(STACK_ID));

        verify(instanceMetaDataService).getNotDeletedAndNotZombieInstanceMetadataByStackId(STACK_ID);
    }

    @Test
    public void testIsCurrentOsUsedOnInstancesShouldReturnFalseWhenAnInstanceUsingADifferentOS() {
        Set<InstanceMetaData> instanceMetaData = createInstanceMetaDataWithOs(OsType.CENTOS7, OsType.RHEL8, OsType.CENTOS7);
        when(instanceMetaDataService.getNotDeletedAndNotZombieInstanceMetadataByStackId(STACK_ID)).thenReturn(instanceMetaData);

        assertEquals(Set.of(OsType.RHEL8, OsType.CENTOS7), underTest.getOSUsedByInstances(STACK_ID));

        verify(instanceMetaDataService).getNotDeletedAndNotZombieInstanceMetadataByStackId(STACK_ID);
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image createImage(String imageId, OsType osType) {
        return Image.builder().withImageId(imageId).withOs(osType.getOs()).withOsType(osType.getOsType()).build();
    }

    private Set<InstanceMetaData> createInstanceMetaDataWithImage(String... imageId) {
        Set<InstanceMetaData> instanceMetaDataSet = new HashSet<>();
        for (int i = 0; i < imageId.length; i++) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setInstanceId("instance-" + i);
            instanceMetaData.setImage(new Json(createImage(imageId[i], OsType.CENTOS7)));
            instanceMetaDataSet.add(instanceMetaData);
        }
        return instanceMetaDataSet;
    }

    private Set<InstanceMetaData> createInstanceMetaDataWithOs(OsType... osTypes) {
        Set<InstanceMetaData> instanceMetaDataSet = new HashSet<>();
        for (int i = 0; i < osTypes.length; i++) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setInstanceId("instance-" + i);
            instanceMetaData.setImage(new Json(createImage("image-id" + i, osTypes[i])));
            instanceMetaDataSet.add(instanceMetaData);
        }
        return instanceMetaDataSet;
    }

}