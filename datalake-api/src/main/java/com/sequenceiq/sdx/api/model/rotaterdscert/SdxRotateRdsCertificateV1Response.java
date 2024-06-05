package com.sequenceiq.sdx.api.model.rotaterdscert;

import static com.sequenceiq.sdx.api.model.ModelDescriptions.DATA_LAKE_CRN;
import static com.sequenceiq.sdx.api.model.ModelDescriptions.SdxRotateRdsCertificateDescription.FLOW_ID;
import static com.sequenceiq.sdx.api.model.ModelDescriptions.SdxRotateRdsCertificateDescription.ROTATE_RDS_CERTIFICATE_ERROR_REASON;
import static com.sequenceiq.sdx.api.model.ModelDescriptions.SdxRotateRdsCertificateDescription.ROTATE_RDS_CERTIFICATE_RESPONSE_TYPE;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxRotateRdsCertificateV1Response {

    @Schema(description = FLOW_ID)
    private FlowIdentifier flowIdentifier;

    @Schema(description = ROTATE_RDS_CERTIFICATE_RESPONSE_TYPE)
    private SdxRotateRdsCertResponseType responseType;

    @Schema(description = ROTATE_RDS_CERTIFICATE_ERROR_REASON)
    private String reason;

    @Schema(description = DATA_LAKE_CRN)
    private String resourceCrn;

    public SdxRotateRdsCertificateV1Response() {
    }

    public SdxRotateRdsCertificateV1Response(SdxRotateRdsCertResponseType responseType, FlowIdentifier flowIdentifier, String reason, String resourceCrn) {
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

    public SdxRotateRdsCertResponseType getResponseType() {
        return responseType;
    }

    public void setResponseType(SdxRotateRdsCertResponseType responseType) {
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
        SdxRotateRdsCertificateV1Response that = (SdxRotateRdsCertificateV1Response) o;
        return Objects.equals(flowIdentifier, that.flowIdentifier) && responseType == that.responseType &&
                Objects.equals(reason, that.reason) && Objects.equals(resourceCrn, that.resourceCrn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flowIdentifier, responseType, reason, resourceCrn);
    }

    @Override
    public String toString() {
        return "RotateRdsCertificateV1Response{" +
                "flowIdentifier=" + flowIdentifier +
                ", responseType=" + responseType +
                ", reason='" + reason + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                '}';
    }
}
