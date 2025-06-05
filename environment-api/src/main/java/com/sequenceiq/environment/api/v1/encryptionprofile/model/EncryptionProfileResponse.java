package com.sequenceiq.environment.api.v1.encryptionprofile.model;

import static com.sequenceiq.environment.api.doc.ModelDescriptions.CRN;

import java.util.Map;
import java.util.Set;

import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.doc.encryptionprofile.EncryptionProfileDescriptor;
import com.sequenceiq.environment.api.doc.encryptionprofile.EncryptionProfileModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = EncryptionProfileDescriptor.ENCRYPTION_PROFILE_NOTES, name = "EncryptionProfileResponse")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EncryptionProfileResponse {

    @Schema(description = ModelDescriptions.NAME, requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 1000, message = "The length of the description cannot be longer than 1000 character")
    @Schema(description = ModelDescriptions.DESCRIPTION)
    private String description;

    @Schema(description = CRN, requiredMode = Schema.RequiredMode.REQUIRED)
    private String crn;

    @Schema(description = EncryptionProfileModelDescription.CREATED, requiredMode = Schema.RequiredMode.REQUIRED)
    private Long created;

    @Schema(description = EncryptionProfileModelDescription.TLS_VERSIONS, requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<String> tlsVersions;

    @Schema(description = EncryptionProfileModelDescription.TLS_CIPHER_SUITES, requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Set<String>> cipherSuites;

    public Set<String> getTlsVersions() {
        return tlsVersions;
    }

    public void setTlsVersions(Set<String> tlsVersions) {
        this.tlsVersions = tlsVersions;
    }

    public Map<String, Set<String>> getCipherSuites() {
        return cipherSuites;
    }

    public void setCipherSuites(Map<String, Set<String>> cipherSuites) {
        this.cipherSuites = cipherSuites;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    @Override
    public String toString() {
        return "EncryptionProfileRequest{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", crn='" + crn + '\'' +
                ", tlsVersions='" + tlsVersions + '\'' +
                ", cipherSuites='" + cipherSuites + '\'' +
                '}';
    }
}
