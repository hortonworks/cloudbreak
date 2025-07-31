package com.sequenceiq.cloudbreak.tls;

import java.util.Set;

import com.sequenceiq.common.api.encryptionprofile.TlsVersion;

public record CipherSuite(String name, String ianaName, Set<TlsVersion> tlsVersions) {
    public static CipherSuite cipherSuite(String name, String ianaName, Set<TlsVersion> tlsVersions) {
        return new CipherSuite(name, ianaName, tlsVersions);
    }
}