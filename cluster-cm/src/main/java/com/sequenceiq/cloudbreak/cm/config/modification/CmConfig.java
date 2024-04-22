package com.sequenceiq.cloudbreak.cm.config.modification;

public record CmConfig(CmServiceType serviceType, String key, String value) {

    @Override
    public String toString() {
        return "CmConfig{" +
                "serviceType=" + serviceType +
                ", key='" + key + '\'' +
                '}';
    }
}
