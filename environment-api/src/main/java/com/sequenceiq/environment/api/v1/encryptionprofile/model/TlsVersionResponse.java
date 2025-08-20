package com.sequenceiq.environment.api.v1.encryptionprofile.model;

import java.util.List;

import com.sequenceiq.environment.api.doc.encryptionprofile.EncryptionProfileModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TlsVersionResponse", description = "TLS version and cipher suites available for this version")
public class TlsVersionResponse {

    @Schema(description = EncryptionProfileModelDescription.TLS_VERSIONS)
    private String tlsVersion;

    @Schema(description = EncryptionProfileModelDescription.CIPHER_SUITES)
    private List<String> cipherSuites;

    @Schema(description = EncryptionProfileModelDescription.RECOMMENDED_CIPHER_SUITES)
    private List<String> recommended;

    public TlsVersionResponse(String tlsVersion, List<String> cipherSuites, List<String> recommended) {
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

    public List<String> getCipherSuites() {
        return cipherSuites;
    }

    public void setCipherSuites(List<String> cipherSuites) {
        this.cipherSuites = cipherSuites;
    }

    public List<String> getRecommended() {
        return recommended;
    }

    public void setRecommended(List<String> recommended) {
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
