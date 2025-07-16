package com.sequenceiq.cloudbreak.tls;

import java.util.Set;

import com.sequenceiq.common.api.encryptionprofile.TlsVersion;

public class CipherSuite {

    private final String name;

    private final String ianaName;

    private final Set<TlsVersion> tlsVersions;

    CipherSuite(String name, String ianaName, Set<TlsVersion> tlsVersions) {
        this.name = name;
        this.ianaName = ianaName;
        this.tlsVersions = tlsVersions;
    }

    public String getName() {
        return name;
    }

    public String getIanaName() {
        return ianaName;
    }

    public Set<TlsVersion> getTlsVersions() {
        return tlsVersions;
    }
}