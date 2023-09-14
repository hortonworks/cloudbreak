package com.sequenceiq.environment.api.v1.environment.model.response;

import java.util.List;

public class SupportedOperatingSystemResponse {

    private List<OsTypeResponse> osTypes;

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
