package com.sequenceiq.environment.api.v1.environment;

import org.springframework.stereotype.Component;

import com.sequenceiq.common.model.OsType;
import com.sequenceiq.environment.api.v1.environment.model.response.OsTypeResponse;

@Component
public class OsTypeToOsTypeResponseConverter {

    public OsTypeResponse convert(OsType osType) {
        OsTypeResponse osTypeResponse = new OsTypeResponse();
        osTypeResponse.setOs(osType.getOs());
        osTypeResponse.setName(osType.getName());
        osTypeResponse.setShortName(osType.getShortName());
        return osTypeResponse;
    }
}
