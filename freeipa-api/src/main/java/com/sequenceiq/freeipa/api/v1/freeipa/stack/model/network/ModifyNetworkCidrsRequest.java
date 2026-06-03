package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.network;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ModifyNetworkCidrsRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ModifyNetworkCidrsRequest(
        @NotNull
        @NotEmpty
        @Schema(description = FreeIpaModelDescriptions.NetworkModelDescription.NETWORK_CIDRS, requiredMode = Schema.RequiredMode.REQUIRED)
        List<String> networkCidrs) {
}
