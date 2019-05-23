package com.sequenceiq.distrox.v1.distrox.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.MaintenanceModeV4Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXMaintenanceModeV1Request;

@Component
public class DistroXMaintenanceModeV1ToMainenanceModeV4Converter {

    public MaintenanceModeV4Request convert(DistroXMaintenanceModeV1Request source) {
        MaintenanceModeV4Request response = new MaintenanceModeV4Request();
        response.setStatus(source.getStatus());
        return response;
    }
}
