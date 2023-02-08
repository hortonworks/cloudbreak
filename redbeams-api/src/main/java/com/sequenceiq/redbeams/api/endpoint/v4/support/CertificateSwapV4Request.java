package com.sequenceiq.redbeams.api.endpoint.v4.support;

import static com.sequenceiq.redbeams.doc.ModelDescriptions.SupportModelDescriptions.FIRST_CERTIFICATE;
import static com.sequenceiq.redbeams.doc.ModelDescriptions.SupportModelDescriptions.SECOND_CERTIFICATE;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.redbeams.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = ModelDescriptions.SUPPORT_CERTIFICATE_REQUEST)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CertificateSwapV4Request {

    @NotNull
    @ApiModelProperty(value = FIRST_CERTIFICATE, required = true)
    private Boolean firstCert;

    @NotNull
    @ApiModelProperty(value = SECOND_CERTIFICATE, required = true)
    private Boolean secondCert;

    public Boolean getFirstCert() {
        return firstCert;
    }

    public void setFirstCert(Boolean firstCert) {
        this.firstCert = firstCert;
    }

    public Boolean getSecondCert() {
        return secondCert;
    }

    public void setSecondCert(Boolean secondCert) {
        this.secondCert = secondCert;
    }

    @Override
    public String toString() {
        return "CertificateSwapV4Request{" +
                "firstCert='" + firstCert + '\'' +
                ", secondCert='" + secondCert + '\'' +
                '}';
    }
}
