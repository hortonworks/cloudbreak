package com.sequenceiq.environment.api.v1.encryptionprofile.model;

import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.api.encryptionprofile.TlsVersion;
import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.doc.encryptionprofile.EncryptionProfileDescriptor;
import com.sequenceiq.environment.api.doc.encryptionprofile.EncryptionProfileModelDescription;
import com.sequenceiq.environment.api.v1.encryptionprofile.validation.ValidEncryptionProfileRequest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = EncryptionProfileDescriptor.ENCRYPTION_PROFILE_NOTES, name = "EncryptionProfileRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ValidEncryptionProfileRequest
public class EncryptionProfileRequest {

    @Size(max = 100, min = 5, message = "The length of the encryption-profile's name has to be in range of 5 to 100")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The name of the encryption-profile can only contain lowercase alphanumeric characters and hyphens " +
                    "and has start with an alphanumeric character")
    @Schema(description = ModelDescriptions.NAME, requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 1000, message = "The length of the description cannot be longer than 1000 character")
    @Schema(description = ModelDescriptions.DESCRIPTION)
    private String description;

    @Schema(description = EncryptionProfileModelDescription.TLS_VERSIONS, requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<TlsVersion> tlsVersions;

    @Schema(description = EncryptionProfileModelDescription.CIPHER_SUITES)
    private List<String> cipherSuites;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<TlsVersion> getTlsVersions() {
        return tlsVersions;
    }

    public void setTlsVersions(Set<TlsVersion> tlsVersions) {
        this.tlsVersions = tlsVersions;
    }

    public List<String> getCipherSuites() {
        return cipherSuites;
    }

    public void setCipherSuites(List<String> cipherSuites) {
        this.cipherSuites = cipherSuites;
    }

    @Override
    public String toString() {
        return "EncryptionProfileRequest{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", tlsVersions='" + tlsVersions + '\'' +
                ", cipherSuites='" + cipherSuites + '\'' +
                '}';
    }
}
