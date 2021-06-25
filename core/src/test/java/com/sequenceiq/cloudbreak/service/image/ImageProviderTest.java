package com.sequenceiq.cloudbreak.service.image;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

@RunWith(MockitoJUnitRunner.class)
public class ImageProviderTest {

    private static final long STACK_ID = 1L;

    private static final String NEW_IMAGE = "new-image";

    private static final String OLD_IMAGE = "old-image";

    @InjectMocks
    private ImageProvider underTest;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Test
    public void testFilterCurrentImageShouldReturnTrueWhenThereAreInstanceWithOtherImage() {
        Set<InstanceMetaData> instanceMetaData = Set.of(createInstanceMetaData(NEW_IMAGE), createInstanceMetaData(OLD_IMAGE));
        when(instanceMetaDataService.getNotDeletedInstanceMetadataByStackId(STACK_ID)).thenReturn(instanceMetaData);

        assertTrue(underTest.filterCurrentImage(STACK_ID, NEW_IMAGE));

        verify(instanceMetaDataService).getNotDeletedInstanceMetadataByStackId(STACK_ID);
    }

    @Test
    public void testFilterCurrentImageShouldReturnTrueWhenThereAreNoInstanceWithOtherImage() {
        Set<InstanceMetaData> instanceMetaData = Set.of(createInstanceMetaData(NEW_IMAGE), createInstanceMetaData(NEW_IMAGE));
        when(instanceMetaDataService.getNotDeletedInstanceMetadataByStackId(STACK_ID)).thenReturn(instanceMetaData);

        assertFalse(underTest.filterCurrentImage(STACK_ID, NEW_IMAGE));

        verify(instanceMetaDataService).getNotDeletedInstanceMetadataByStackId(STACK_ID);
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image createImage(String imageId) {
        return new com.sequenceiq.cloudbreak.cloud.model.Image(null, null, null, null, null, null, imageId, null);
    }

    private InstanceMetaData createInstanceMetaData(String imageId) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setImage(new Json(createImage(imageId)));
        return instanceMetaData;
    }

}