package com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response;

import com.sequenceiq.common.model.annotations.Immutable;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModelProperty;

@Immutable
public class CertificateV4Response {

    @ApiModelProperty(StackModelDescription.CERTIFICATE)
    private String serverCert;

    @ApiModelProperty(StackModelDescription.CLIENT_KEY)
    private String clientKeyPath;

    @ApiModelProperty(StackModelDescription.CLIENT_CERT)
    private String clientCertPath;

    public CertificateV4Response() {
    }

    public CertificateV4Response(String serverCert, String clientKeyPath, String clientCertPath) {
        this.serverCert = serverCert;
        this.clientKeyPath = clientKeyPath;
        this.clientCertPath = clientCertPath;
    }

    public String getServerCert() {
        return serverCert;
    }

    public String getClientKeyPath() {
        return clientKeyPath;
    }

    public String getClientCertPath() {
        return clientCertPath;
    }
}
