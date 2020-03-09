package com.sequenceiq.distrox.v1.distrox.converter.cli.distrox;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;

public interface DistroXV1RequestToCliRequestConverter {
    Object convert(DistroXV1Request source);

    CloudPlatform supportedPlatform();
}
