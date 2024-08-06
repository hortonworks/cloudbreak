package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses;

import java.io.Serializable;
import java.util.Set;

import com.sequenceiq.cloudbreak.common.domain.SslCertStatus;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.doc.ModelDescriptions;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServer;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.SSL_CONFIG_RESPONSE)
public class SslConfigV4Response implements Serializable {

    @Schema(description = DatabaseServer.SSL_CERTIFICATES)
    private Set<String> sslCertificates;

    @Schema(description = DatabaseServer.SSL_CERTIFICATE_TYPE)
    private SslCertificateType sslCertificateType = SslCertificateType.NONE;

    @Schema(description = DatabaseServer.SSL_MODE)
    private SslMode sslMode = SslMode.DISABLED;

    @Schema(description = DatabaseServer.SSL_CERTIFICATE_ACTIVE_VERSION)
    private int sslCertificateActiveVersion;

    @Schema(description = DatabaseServer.SSL_CERTIFICATE_HIGHEST_AVAILABLE_VERSION)
    private int sslCertificateHighestAvailableVersion;

    @Schema(description = DatabaseServer.SSL_CERTIFICATE_ACTIVE_CLOUD_PROVIDER_IDENTIFIER)
    private String sslCertificateActiveCloudProviderIdentifier;

    @Schema(description = DatabaseServer.SSL_CERTIFICATE_STATUS)
    private SslCertStatus sslCertificatesStatus;

    @Schema(description = DatabaseServer.SSL_CERTIFICATE_EXPIRATION_DATE_AS_LONG)
    private long sslCertificateExpirationDate;

    @Schema(description = DatabaseServer.SSL_CERTIFICATE_EXPIRATION_DATE_AS_STRING)
    private String sslCertificateExpirationDateAsDateString;

    public Set<String> getSslCertificates() {
        return sslCertificates;
    }

    public void setSslCertificates(Set<String> sslCertificates) {
        this.sslCertificates = sslCertificates;
    }

    public SslCertificateType getSslCertificateType() {
        return sslCertificateType;
    }

    public void setSslCertificateType(SslCertificateType sslCertificateType) {
        this.sslCertificateType = sslCertificateType;
    }

    public SslMode getSslMode() {
        return sslMode;
    }

    public void setSslMode(SslMode sslMode) {
        this.sslMode = sslMode;
    }

    public int getSslCertificateActiveVersion() {
        return sslCertificateActiveVersion;
    }

    public void setSslCertificateActiveVersion(int sslCertificateActiveVersion) {
        this.sslCertificateActiveVersion = sslCertificateActiveVersion;
    }

    public int getSslCertificateHighestAvailableVersion() {
        return sslCertificateHighestAvailableVersion;
    }

    public void setSslCertificateHighestAvailableVersion(int sslCertificateHighestAvailableVersion) {
        this.sslCertificateHighestAvailableVersion = sslCertificateHighestAvailableVersion;
    }

    public String getSslCertificateActiveCloudProviderIdentifier() {
        return sslCertificateActiveCloudProviderIdentifier;
    }

    public void setSslCertificateActiveCloudProviderIdentifier(String sslCertificateActiveCloudProviderIdentifier) {
        this.sslCertificateActiveCloudProviderIdentifier = sslCertificateActiveCloudProviderIdentifier;
    }

    public long getSslCertificateExpirationDate() {
        return sslCertificateExpirationDate;
    }

    public void setSslCertificateExpirationDate(long sslCertificateExpirationDate) {
        this.sslCertificateExpirationDate = sslCertificateExpirationDate;
    }

    public String getSslCertificateExpirationDateAsDateString() {
        return sslCertificateExpirationDateAsDateString;
    }

    public void setSslCertificateExpirationDateAsDateString(String sslCertificateExpirationDateAsDateString) {
        this.sslCertificateExpirationDateAsDateString = sslCertificateExpirationDateAsDateString;
    }

    public SslCertStatus getSslCertificatesStatus() {
        return sslCertificatesStatus;
    }

    public void setSslCertificatesStatus(SslCertStatus sslCertificatesStatus) {
        this.sslCertificatesStatus = sslCertificatesStatus;
    }
}
