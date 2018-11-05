package com.sequenceiq.cloudbreak.api.model;

import com.sequenceiq.cloudbreak.api.model.annotations.Immutable;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModelProperty;

@Immutable
public class CertificateResponse {

    @ApiModelProperty(StackModelDescription.CERTIFICATE)
    private String serverCert;

    @ApiModelProperty(StackModelDescription.CLIENT_KEY)
    private String clientKeyPath;

    @ApiModelProperty(StackModelDescription.CLIENT_CERT)
    private String clientCertPath;

    public CertificateResponse() {
    }

    public CertificateResponse(String serverCert, String clientKeyPath, String clientCertPath) {
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
