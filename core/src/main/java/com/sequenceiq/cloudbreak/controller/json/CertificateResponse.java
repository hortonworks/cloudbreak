package com.sequenceiq.cloudbreak.controller.json;


import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class CertificateResponse {

    @ApiModelProperty(value = ModelDescriptions.StackModelDescription.CERTIFICATE)
    private String certificate;

    public CertificateResponse(String certificate) {
        this.certificate = certificate;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }
}
