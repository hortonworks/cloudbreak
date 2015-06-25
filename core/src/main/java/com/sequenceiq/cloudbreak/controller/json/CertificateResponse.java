package com.sequenceiq.cloudbreak.controller.json;


import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class CertificateResponse {

    @ApiModelProperty(value = ModelDescriptions.StackModelDescription.CERTIFICATE)
    private byte[] certificate;

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
