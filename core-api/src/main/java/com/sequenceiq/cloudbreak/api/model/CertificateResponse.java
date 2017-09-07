package com.sequenceiq.cloudbreak.api.model;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class CertificateResponse {

    @ApiModelProperty(StackModelDescription.CERTIFICATE)
    private byte[] serverCert;

    @ApiModelProperty(StackModelDescription.CLIENT_KEY)
    private byte[] clientKey;

    @ApiModelProperty(StackModelDescription.CLIENT_CERT)
    private byte[] clientCert;

    public CertificateResponse() {
    }

    public CertificateResponse(byte[] serverCert, byte[] clientKey, byte[] clientCert) {
        this.serverCert = serverCert;
        this.clientKey = clientKey;
        this.clientCert = clientCert;
    }

    public byte[] getServerCert() {
        return serverCert;
    }

    public byte[] getClientKey() {
        return clientKey;
    }

    public byte[] getClientCert() {
        return clientCert;
    }
}
