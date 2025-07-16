package com.sequenceiq.environment.api.v1.encryptionprofile.model;

import java.util.Set;

import com.sequenceiq.environment.api.doc.encryptionprofile.EncryptionProfileModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TlsVersionResponse", description = "TLS version and cipher suites available for this version")
public class TlsVersionResponse {

    @Schema(description = EncryptionProfileModelDescription.TLS_VERSIONS)
    private String tlsVersion;

    @Schema(description = EncryptionProfileModelDescription.CIPHER_SUITES)
    private Set<String> cipherSuites;

    @Schema(description = EncryptionProfileModelDescription.RECOMMENDED_CIPHER_SUITES)
    private Set<String> recommended;

    public TlsVersionResponse(String tlsVersion, Set<String> cipherSuites, Set<String> recommended) {
        this.tlsVersion = tlsVersion;
        this.cipherSuites = cipherSuites;
        this.recommended = recommended;
    }

    public String getTlsVersion() {
        return tlsVersion;
    }

    public void setTlsVersion(String tlsVersion) {
        this.tlsVersion = tlsVersion;
    }

    public Set<String> getCipherSuites() {
        return cipherSuites;
    }

    public void setCipherSuites(Set<String> cipherSuites) {
        this.cipherSuites = cipherSuites;
    }

    public Set<String> getRecommended() {
        return recommended;
    }

    public void setRecommended(Set<String> recommended) {
        this.recommended = recommended;
    }

    @Override
    public String toString() {
        return "TlsVersionResponse{" +
                "tlsVersion='" + tlsVersion + '\'' +
                ", cipherSuites=" + cipherSuites +
                ", recommended=" + recommended +
                '}';
    }
}
