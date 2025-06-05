package com.sequenceiq.cloudbreak.tls;

public class CipherSuite {

    private final String name;

    private final String ianaName;

    CipherSuite(String name, String ianaName) {
        this.name = name;
        this.ianaName = ianaName;
    }

    public String getName() {
        return name;
    }

    public String getIanaName() {
        return ianaName;
    }
}