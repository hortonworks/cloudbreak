package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.model.CcmUpgradeResponseType;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.UpgradeCcmDescription;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StackCcmUpgradeV4Response {

    @Schema(description = UpgradeCcmDescription.FLOW_ID)
    private FlowIdentifier flowIdentifier;

    @Schema(description = UpgradeCcmDescription.CCM_UPGRADE_RESPONSE_TYPE)
    private CcmUpgradeResponseType responseType;

    @Schema(description = UpgradeCcmDescription.CCM_UPGRADE_ERROR_REASON)
    private String reason;

    @Schema(description = StackModelDescription.CRN)
    private String resourceCrn;

    public StackCcmUpgradeV4Response() {
    }

    public StackCcmUpgradeV4Response(CcmUpgradeResponseType responseType, FlowIdentifier flowIdentifier, String reason, String resourceCrn) {
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

    public CcmUpgradeResponseType getResponseType() {
        return responseType;
    }

    public void setResponseType(CcmUpgradeResponseType responseType) {
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
        StackCcmUpgradeV4Response that = (StackCcmUpgradeV4Response) o;
        return Objects.equals(flowIdentifier, that.flowIdentifier) && responseType == that.responseType &&
                Objects.equals(reason, that.reason) && Objects.equals(resourceCrn, that.resourceCrn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flowIdentifier, responseType, reason, resourceCrn);
    }

    @Override
    public String toString() {
        return "StackCcmUpgradeV4Response{" +
                "flowIdentifier=" + flowIdentifier +
                ", responseType=" + responseType +
                ", reason='" + reason + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                '}';
    }
}
