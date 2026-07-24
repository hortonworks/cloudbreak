package com.sequenceiq.freeipa.service.freeipa.config;

import com.sequenceiq.common.api.encryptionprofile.TlsVersion;

/**
 * TLS protocol version tokens as expected by the 389-ds directory server / NSS for {@code sslVersionMin} /
 * {@code sslVersionMax} on {@code cn=encryption} (e.g. {@code TLS1.2}, {@code TLS1.3}). Note these differ from the
 * {@code TLSv1.2} form used by nginx and {@link TlsVersion}.
 */
public enum DirectoryServerTlsVersion {

    TLS_1_2(TlsVersion.TLS_1_2, "TLS1.2"),
    TLS_1_3(TlsVersion.TLS_1_3, "TLS1.3");

    private final TlsVersion tlsVersion;

    private final String version;

    DirectoryServerTlsVersion(TlsVersion tlsVersion, String version) {
        this.tlsVersion = tlsVersion;
        this.version = version;
    }

    public TlsVersion getTlsVersion() {
        return tlsVersion;
    }

    public String getVersion() {
        return version;
    }
}
