package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DirectionalTrustSetupCommandsV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DirectionalTrustSetupCommandsResponse extends TrustSetupCommandsBase {

    @NotEmpty
    @Schema(description = FreeIpaModelDescriptions.CrossRealmTrustModelDescriptions.KDC_TYPE, requiredMode = Schema.RequiredMode.REQUIRED)
    private String kdcType;

    @Schema(description = "One-way trust setup commands", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private TrustSetupCommandsResponse oneWay;

    @Schema(description = "Two-way trust setup commands", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private TrustSetupCommandsResponse twoWay;

    public String getKdcType() {
        return kdcType;
    }

    public void setKdcType(String kdcType) {
        this.kdcType = kdcType;
    }

    public TrustSetupCommandsResponse getOneWay() {
        return oneWay;
    }

    public void setOneWay(TrustSetupCommandsResponse oneWay) {
        this.oneWay = oneWay;
    }

    public TrustSetupCommandsResponse getTwoWay() {
        return twoWay;
    }

    public void setTwoWay(TrustSetupCommandsResponse twoWay) {
        this.twoWay = twoWay;
    }

    @Override
    public String toString() {
        return "DirectionalTrustSetupCommandsResponse{" +
                "kdcType='" + kdcType + '\'' +
                ", oneWay=" + oneWay +
                ", twoWay=" + twoWay +
                "} " + super.toString();
    }
}
