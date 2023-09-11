package com.sequenceiq.environment.api.v1.environment.model.response;

import java.util.List;

import com.sequenceiq.common.model.OsType;

public class SupportedOperatingSystemResponse {

    private List<OsType> osTypes;

    private String defaultOs;

    public List<OsType> getOsTypes() {
        return osTypes;
    }

    public void setOsTypes(List<OsType> osTypes) {
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
