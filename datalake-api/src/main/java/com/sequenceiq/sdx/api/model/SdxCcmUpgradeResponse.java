package com.sequenceiq.sdx.api.model;

import java.util.Objects;

import com.sequenceiq.flow.api.model.FlowIdentifier;

public class SdxCcmUpgradeResponse {

    private FlowIdentifier flowIdentifier;

    private CcmUpgradeResponseType responseType;

    private String reason;

    public SdxCcmUpgradeResponse() {
    }

    public SdxCcmUpgradeResponse(CcmUpgradeResponseType responseType, FlowIdentifier flowIdentifier, String reason) {
        this.responseType = responseType;
        this.flowIdentifier = flowIdentifier;
        this.reason = reason;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SdxCcmUpgradeResponse that = (SdxCcmUpgradeResponse) o;
        return Objects.equals(flowIdentifier, that.flowIdentifier) && responseType == that.responseType && Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flowIdentifier, responseType, reason);
    }

    @Override
    public String toString() {
        return "SdxCcmUpgradeResponse{" +
                "flowIdentifier=" + flowIdentifier +
                ", responseType=" + responseType +
                ", reason='" + reason + '\'' +
                '}';
    }
}
