package com.sequenceiq.cloudbreak.domain;

import java.util.List;

public class BlueprintForUpgrade {

    private List<String> services;

    private String gaVersion;

    public BlueprintForUpgrade() {
    }

    public BlueprintForUpgrade(List<String> services, String gaVersion) {
        this.services = services;
        this.gaVersion = gaVersion;
    }

    public List<String> getServices() {
        return services;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }

    public String getGaVersion() {
        return gaVersion;
    }

    public void setGaVersion(String gaVersion) {
        this.gaVersion = gaVersion;
    }

    @Override
    public String toString() {
        return "BlueprintForUpgrade{" +
                "services=" + services +
                ", gaVersion='" + gaVersion + '\'' +
                '}';
    }
}
