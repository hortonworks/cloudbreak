package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateNetworkCidrsRequest(
        @NotNull
        @NotEmpty
        @Schema(description = "The network CIDR list to apply to all clusters in the environment", requiredMode = Schema.RequiredMode.REQUIRED)
        List<String> networkCidrs) {
}
