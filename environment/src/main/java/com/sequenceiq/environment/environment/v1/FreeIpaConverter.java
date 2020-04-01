package com.sequenceiq.environment.environment.v1;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.environment.model.response.FreeIpaResponse;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationDto;

@Component
public class FreeIpaConverter {

    public FreeIpaResponse convert(FreeIpaCreationDto freeIpaCreation) {
        if (freeIpaCreation == null) {
            return null;
        } else {
            FreeIpaResponse response = new FreeIpaResponse();
            response.setInstanceCountByGroup(freeIpaCreation.getInstanceCountByGroup());
            response.setSpotPercentage(freeIpaCreation.getSpotPercentage());
            return response;
        }
    }
}
