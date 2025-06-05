package com.sequenceiq.common.api.encryptionprofile;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TlsVersion {

    TLS_1_2("TLSv1.2"),

    TLS_1_3("TLSv1.3");

    private final String version;

    TlsVersion(String version) {
        this.version = version;
    }

    @JsonValue
    public String getVersion() {
        return version;
    }

    @JsonCreator
    public static TlsVersion fromString(String version) {
        for (TlsVersion tlsVersion : TlsVersion.values()) {
            if (tlsVersion.getVersion().equalsIgnoreCase(version)) {
                return tlsVersion;
            }
        }
        throw new IllegalArgumentException("Unknown TLS version: " + version);
    }
}
