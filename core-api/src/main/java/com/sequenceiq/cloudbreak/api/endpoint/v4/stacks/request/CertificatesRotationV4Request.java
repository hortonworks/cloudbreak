package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import java.util.Objects;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.CertificateRotationType;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class CertificatesRotationV4Request implements JsonEntity {
    @ApiModelProperty(value = ClusterModelDescription.CERTIFICATE_ROTATION_TYPE, allowableValues = "HOST_CERTS")
    private CertificateRotationType certificateRotationType = CertificateRotationType.HOST_CERTS;

    public CertificateRotationType getRotateCertificatesType() {
        return certificateRotationType;
    }

    public void setRotateCertificatesType(CertificateRotationType certificateRotationType) {
        this.certificateRotationType = certificateRotationType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CertificatesRotationV4Request that = (CertificatesRotationV4Request) o;
        return certificateRotationType == that.certificateRotationType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(certificateRotationType);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CertificatesRotationV4Request.class.getSimpleName() + "[", "]")
                .add("certificateRotationType=" + certificateRotationType)
                .toString();
    }
}
