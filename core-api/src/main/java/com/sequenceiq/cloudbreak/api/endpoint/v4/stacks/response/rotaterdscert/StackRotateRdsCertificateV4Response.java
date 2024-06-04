package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.rotaterdscert;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.model.RotateRdsCertResponseType;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RotateRdsCertificateDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StackRotateRdsCertificateV4Response {

    @Schema(description = RotateRdsCertificateDescription.FLOW_ID)
    private FlowIdentifier flowIdentifier;

    @Schema(description = RotateRdsCertificateDescription.ROTATE_RDS_CERTIFICATE_RESPONSE_TYPE)
    private RotateRdsCertResponseType responseType;

    @Schema(description = RotateRdsCertificateDescription.ROTATE_RDS_CERTIFICATE_ERROR_REASON)
    private String reason;

    @Schema(description = StackModelDescription.CRN)
    private String resourceCrn;

    public StackRotateRdsCertificateV4Response() {
    }

    public StackRotateRdsCertificateV4Response(RotateRdsCertResponseType responseType, FlowIdentifier flowIdentifier, String reason, String resourceCrn) {
        this.responseType = responseType;
        this.flowIdentifier = flowIdentifier;
        this.reason = reason;
        this.resourceCrn = resourceCrn;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setFlowIdentifier(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    public RotateRdsCertResponseType getResponseType() {
        return responseType;
    }

    public void setResponseType(RotateRdsCertResponseType responseType) {
        this.responseType = responseType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        StackRotateRdsCertificateV4Response that = (StackRotateRdsCertificateV4Response) o;
        return Objects.equals(flowIdentifier, that.flowIdentifier) && responseType == that.responseType &&
                Objects.equals(reason, that.reason) && Objects.equals(resourceCrn, that.resourceCrn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flowIdentifier, responseType, reason, resourceCrn);
    }

    @Override
    public String toString() {
        return "StackRotateRdsCertificateV4Response{" +
                "flowIdentifier=" + flowIdentifier +
                ", responseType=" + responseType +
                ", reason='" + reason + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                '}';
    }
}
