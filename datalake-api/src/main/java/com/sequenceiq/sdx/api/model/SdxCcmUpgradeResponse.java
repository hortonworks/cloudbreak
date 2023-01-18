package com.sequenceiq.sdx.api.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxCcmUpgradeResponse {

    @Schema(description = ModelDescriptions.FLOW_IDENTIFIER)
    private FlowIdentifier flowIdentifier;

    @Schema(description = ModelDescriptions.CCM_UPGRADE_RESPONSE_TYPE)
    private CcmUpgradeResponseType responseType;

    @Schema(description = ModelDescriptions.CCM_UPGRADE_ERROR_REASON)
    private String reason;

    @Schema(description = ModelDescriptions.DATA_LAKE_CRN)
    private String resourceCrn;

    public SdxCcmUpgradeResponse() {
    }

    public SdxCcmUpgradeResponse(CcmUpgradeResponseType responseType, FlowIdentifier flowIdentifier, String reason, String resourceCrn) {
        this.responseType = responseType;
        this.flowIdentifier = flowIdentifier;
        this.reason = reason;
        this.resourceCrn = resourceCrn;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setFlowIdentifier(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    public CcmUpgradeResponseType getResponseType() {
        return responseType;
    }

    public void setResponseType(CcmUpgradeResponseType responseType) {
        this.responseType = responseType;
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
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SdxCcmUpgradeResponse that = (SdxCcmUpgradeResponse) o;
        return Objects.equals(flowIdentifier, that.flowIdentifier)
                && responseType == that.responseType
                && Objects.equals(reason, that.reason)
                && Objects.equals(resourceCrn, that.resourceCrn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flowIdentifier, responseType, reason, resourceCrn);
    }

    @Override
    public String toString() {
        return "SdxCcmUpgradeResponse{"
                + "flowIdentifier=" + flowIdentifier
                + ", responseType=" + responseType
                + ", reason='" + reason
                + ", resourceCrn='" + resourceCrn + '\'' + '}';
    }
}
