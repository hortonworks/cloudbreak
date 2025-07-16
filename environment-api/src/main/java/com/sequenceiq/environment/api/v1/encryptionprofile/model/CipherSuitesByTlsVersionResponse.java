package com.sequenceiq.environment.api.v1.encryptionprofile.model;

import java.util.Set;

import com.sequenceiq.environment.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CipherSuitesByTlsVersionResponse", description = "Wrapper which contains multiple TlsVersionResponse")
public class CipherSuitesByTlsVersionResponse {

    @Schema(description = ModelDescriptions.DESCRIPTION)
    private Set<TlsVersionResponse> tlsVersions;

    public CipherSuitesByTlsVersionResponse(Set<TlsVersionResponse> tlsVersions) {
        this.tlsVersions = tlsVersions;
    }

    public Set<TlsVersionResponse> getTlsVersions() {
        return tlsVersions;
    }

    public void setTlsVersions(Set<TlsVersionResponse> tlsVersions) {
        this.tlsVersions = tlsVersions;
    }

    @Override
    public String toString() {
        return "CipherSuitesByTlsVersionResponse{" +
                "tlsVersions=" + tlsVersions +
                '}';
    }
}
