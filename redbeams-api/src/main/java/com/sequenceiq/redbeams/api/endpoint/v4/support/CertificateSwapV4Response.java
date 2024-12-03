package com.sequenceiq.redbeams.api.endpoint.v4.support;

import static com.sequenceiq.redbeams.doc.ModelDescriptions.SupportModelDescriptions.CERTIFICATES;

import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.redbeams.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.SUPPORT_CERTIFICATE_RESPONSE)
@JsonInclude(Include.NON_NULL)
public class CertificateSwapV4Response {

    @Schema(description = CERTIFICATES, requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<X509Certificate> certificates = new HashSet<>();

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
