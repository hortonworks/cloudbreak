package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.redbeams.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "SslCertificateEntryResponse")
public class SslCertificateEntryResponse {

    @Schema(description = ModelDescriptions.SslCertificate.VERSION, requiredMode = Schema.RequiredMode.REQUIRED)
    private int version;

    @Schema(description = ModelDescriptions.SslCertificate.CLOUDKEY)
    private String cloudKey;

    @Schema(description = ModelDescriptions.SslCertificate.CLOUDPROVIDERIDENTIFIER)
    private String cloudProviderIdentifier;

    @Schema(description = ModelDescriptions.SslCertificate.CLOUDPLATFORM)
    private String cloudPlatform;

    @Schema(description = ModelDescriptions.SslCertificate.CERTPEM)
    private String certPem;

    @Schema(description = ModelDescriptions.SslCertificate.FINGERPRINT)
    private String fingerprint;

    @Schema(description = ModelDescriptions.SslCertificate.EXPIRATIONDATE, requiredMode = Schema.RequiredMode.REQUIRED)
    private long expirationDate;

    @Schema(description = ModelDescriptions.SslCertificate.DEPRECATED, requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean deprecated;

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getCloudKey() {
        return cloudKey;
    }

    public void setCloudKey(String cloudKey) {
        this.cloudKey = cloudKey;
    }

    public String getCloudProviderIdentifier() {
        return cloudProviderIdentifier;
    }

    public void setCloudProviderIdentifier(String cloudProviderIdentifier) {
        this.cloudProviderIdentifier = cloudProviderIdentifier;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public String getCertPem() {
        return certPem;
    }

    public void setCertPem(String certPem) {
        this.certPem = certPem;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public long getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(long expirationDate) {
        this.expirationDate = expirationDate;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    // @formatter:off
    // CHECKSTYLE:OFF
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SslCertificateEntryResponse that = (SslCertificateEntryResponse) o;
        return version == that.version
                && expirationDate == that.expirationDate
                && deprecated == that.deprecated
                && Objects.equals(cloudKey, that.cloudKey)
                && Objects.equals(cloudProviderIdentifier, that.cloudProviderIdentifier)
                && Objects.equals(cloudPlatform, that.cloudPlatform)
                && Objects.equals(certPem, that.certPem)
                && Objects.equals(fingerprint, that.fingerprint);
    }
    // @formatter:on
    // CHECKSTYLE:on

    @Override
    public int hashCode() {
        return Objects.hash(version, cloudKey, cloudProviderIdentifier, cloudPlatform, certPem, fingerprint, expirationDate, deprecated);
    }
}
