package com.sequenceiq.cloudbreak.api.endpoint.v4.util.base;

import com.sequenceiq.authorization.RightsConstants;

public enum RightV4 {
    DISTROX_READ(RightsConstants.DATAHUB_RESOURCE, RightsConstants.READ_ACTION),
    DISTROX_WRITE(RightsConstants.DATAHUB_RESOURCE, RightsConstants.WRITE_ACTION),
    SDX_READ(RightsConstants.DATALAKE_RESOURCE, RightsConstants.READ_ACTION),
    SDX_WRITE(RightsConstants.DATALAKE_RESOURCE, RightsConstants.WRITE_ACTION),
    ENVIRONMENT_READ(RightsConstants.ENVIRONMENTS_RESOURCE, RightsConstants.READ_ACTION),
    ENVIRONMENT_WRITE(RightsConstants.ENVIRONMENTS_RESOURCE, RightsConstants.WRITE_ACTION);

    private String resource;

    private String action;

    RightV4(String resource, String action) {
        this.resource = resource;
        this.action = action;
    }

    public String getResource() {
        return resource;
    }

    public String getAction() {
        return action;
    }
}
