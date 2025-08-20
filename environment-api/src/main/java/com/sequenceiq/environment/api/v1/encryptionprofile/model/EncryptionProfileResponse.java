package com.sequenceiq.environment.api.v1.encryptionprofile.model;

import static com.sequenceiq.environment.api.doc.ModelDescriptions.CRN;

import java.util.List;
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
    private Map<String, List<String>> cipherSuites;

    @Schema(description = EncryptionProfileModelDescription.CLOUDERA_TLS_CIPHER_SUITES, requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, List<String>> clouderaInternalCipherSuites;

    @Schema(description = EncryptionProfileModelDescription.STATUS, requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    public Set<String> getTlsVersions() {
        return tlsVersions;
    }

    public void setTlsVersions(Set<String> tlsVersions) {
        this.tlsVersions = tlsVersions;
    }

    public Map<String, List<String>> getCipherSuites() {
        return cipherSuites;
    }

    public void setCipherSuites(Map<String, List<String>> cipherSuites) {
        this.cipherSuites = cipherSuites;
    }

    public Map<String, List<String>> getClouderaInternalCipherSuites() {
        return clouderaInternalCipherSuites;
    }

    public void setClouderaInternalCipherSuites(Map<String, List<String>> clouderaInternalCipherSuites) {
        this.clouderaInternalCipherSuites = clouderaInternalCipherSuites;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "EncryptionProfileRequest{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", crn='" + crn + '\'' +
                ", tlsVersions='" + tlsVersions + '\'' +
                ", cipherSuites='" + cipherSuites + '\'' +
                ", clouderaInternalCipherSuites='" + clouderaInternalCipherSuites + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
