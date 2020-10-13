package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses;

import java.io.Serializable;
import java.util.Set;

import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = ModelDescriptions.SSL_CONFIG_RESPONSE)
public class SslConfigV4Response implements Serializable {

    @ApiModelProperty(ModelDescriptions.DatabaseServer.SSL_CERTIFICATES)
    private Set<String> sslCertificates;

    @ApiModelProperty(ModelDescriptions.DatabaseServer.SSL_CERTIFICATE_TYPE)
    private SslCertificateType sslCertificateType = SslCertificateType.NONE;

    @ApiModelProperty(ModelDescriptions.DatabaseServer.SSL_CONFIG)
    private SslMode sslMode = SslMode.DISABLED;

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
}
