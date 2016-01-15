package com.sequenceiq.cloudbreak.api.model;


import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import io.swagger.annotations.ApiModelProperty;

public class CertificateResponse {

    @ApiModelProperty(value = StackModelDescription.CERTIFICATE)
    private byte[] certificate;

    public CertificateResponse() {

    }

    public CertificateResponse(byte[] certificate) {
        this.certificate = certificate;
    }

    public byte[] getCertificate() {
        return certificate;
    }

    public void setCertificate(byte[] certificate) {
        this.certificate = certificate;
    }
}
