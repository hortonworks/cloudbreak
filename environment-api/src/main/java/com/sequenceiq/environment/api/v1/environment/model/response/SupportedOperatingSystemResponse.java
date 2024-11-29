package com.sequenceiq.environment.api.v1.environment.model.response;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public class SupportedOperatingSystemResponse {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<OsTypeResponse> osTypes = new ArrayList<>();

    private String defaultOs;

    public List<OsTypeResponse> getOsTypes() {
        return osTypes;
    }

    public void setOsTypes(List<OsTypeResponse> osTypes) {
        this.osTypes = osTypes;
    }

    public String getDefaultOs() {
        return defaultOs;
    }

    public void setDefaultOs(String defaultOs) {
        this.defaultOs = defaultOs;
    }

    @Override
    public String toString() {
        return "SupportedOperatingSystemResponse{" +
                "osTypes=" + osTypes +
                ", defaultOs='" + defaultOs + '\'' +
                '}';
    }
}
