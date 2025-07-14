package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.converter;

import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesRequest;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesValidationFinishedEvent;
import com.sequenceiq.flow.core.PayloadConverter;

public class AddVolumesRequestToAddVolumesValidationFinishedEventConverter implements PayloadConverter<AddVolumesValidationFinishedEvent> {

    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return AddVolumesRequest.class.isAssignableFrom(sourceClass);
    }

    @Override
    public AddVolumesValidationFinishedEvent convert(Object payload) {
        AddVolumesRequest request = (AddVolumesRequest) payload;
        return new AddVolumesValidationFinishedEvent(
                request.getResourceId(),
                request.getNumberOfDisks(),
                request.getType(),
                request.getSize(),
                request.getCloudVolumeUsageType(),
                request.getInstanceGroup());
    }
}
