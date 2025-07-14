package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesRequest;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesValidationFinishedEvent;

class AddVolumesRequestToAddVolumesValidationFinishedEventConverterTest {

    private final AddVolumesRequestToAddVolumesValidationFinishedEventConverter underTest = new AddVolumesRequestToAddVolumesValidationFinishedEventConverter();

    @Test
    void testConvert() {
        AddVolumesRequest addVolumesRequest = new AddVolumesRequest("selector", 1L, 2L, "gp2", 200L, CloudVolumeUsageType.GENERAL, "test");
        AddVolumesValidationFinishedEvent result = underTest.convert(addVolumesRequest);
        assertEquals(1L, result.getResourceId());
        assertEquals(2L, result.getNumberOfDisks());
        assertEquals("gp2", result.getType());
        assertEquals(200L, result.getSize());
        assertEquals(CloudVolumeUsageType.GENERAL, result.getCloudVolumeUsageType());
        assertEquals("test", result.getInstanceGroup());
    }
}
