package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.model.DefaultOutboundUpgradeResponseType;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.UpgradeOutboundTypeDescription;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StackDefaultOutboundUpgradeV4Response {

    @Schema(description = UpgradeOutboundTypeDescription.FLOW_ID, requiredMode = REQUIRED)
    private FlowIdentifier flowIdentifier;

    @Schema(description = UpgradeOutboundTypeDescription.OUTBOUND_UPGRADE_RESPONSE_TYPE, requiredMode = REQUIRED)
    private DefaultOutboundUpgradeResponseType responseType;

    @Schema(description = UpgradeOutboundTypeDescription.OUTBOUND_UPGRADE_ERROR_REASON)
    private String reason;

    @Schema(description = StackModelDescription.CRN, requiredMode = REQUIRED)
    private String resourceCrn;

    public StackDefaultOutboundUpgradeV4Response() {
    }

    public StackDefaultOutboundUpgradeV4Response(DefaultOutboundUpgradeResponseType responseType, FlowIdentifier flowIdentifier, String reason,
            String resourceCrn) {
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

    public DefaultOutboundUpgradeResponseType getResponseType() {
        return responseType;
    }

    public void setResponseType(DefaultOutboundUpgradeResponseType responseType) {
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
        StackDefaultOutboundUpgradeV4Response that = (StackDefaultOutboundUpgradeV4Response) o;
        return Objects.equals(flowIdentifier, that.flowIdentifier) && responseType == that.responseType &&
                Objects.equals(reason, that.reason) && Objects.equals(resourceCrn, that.resourceCrn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flowIdentifier, responseType, reason, resourceCrn);
    }

    @Override
    public String toString() {
        return "StackDefaultOutboundUpgradeV4Response{" +
                "flowIdentifier=" + flowIdentifier +
                ", responseType=" + responseType +
                ", reason='" + reason + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                '}';
    }
}

