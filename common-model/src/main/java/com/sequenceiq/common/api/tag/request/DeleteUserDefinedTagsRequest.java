package com.sequenceiq.common.api.tag.request;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DeleteUserDefinedTagsRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeleteUserDefinedTagsRequest(
        @NotEmpty
        @Size(max = 64)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Set<@NotBlank @Size(max = 512) String> tagKeys) {
}
