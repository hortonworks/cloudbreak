package com.sequenceiq.redbeams.api.endpoint.v4.support;

import static com.sequenceiq.redbeams.doc.ModelDescriptions.SupportModelDescriptions.CERTIFICATES;

import java.security.cert.X509Certificate;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.redbeams.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = ModelDescriptions.SUPPORT_CERTIFICATE_RESPONSE)
@JsonInclude(Include.NON_NULL)
public class CertificateSwapV4Response {

    @ApiModelProperty(CERTIFICATES)
    private Set<X509Certificate> certificates;

    public Set<X509Certificate> getCertificates() {
        return certificates;
    }

    public void setCertificates(Set<X509Certificate> certificates) {
        this.certificates = certificates;
    }

    @Override
    public String toString() {
        return "CertificateSwapV4Response{" +
                "certificates='" + certificates + '\'' +
                '}';
    }
}
