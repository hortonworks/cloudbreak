package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.resetjvmparams;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResetJvmParamsDiffV4Response {

    @Schema(description = "Configs with their values before recalculation. Only populated for dry-run requests.")
    private List<JvmConfigRecordV4Response> configsBefore = new ArrayList<>();

    @Schema(description = "Configs whose value has changed (or would change) due to the recalculation.")
    private List<JvmConfigRecordV4Response> configsAfter = new ArrayList<>();

    public ResetJvmParamsDiffV4Response() {
    }

    public ResetJvmParamsDiffV4Response(List<JvmConfigRecordV4Response> configsBefore, List<JvmConfigRecordV4Response> configsAfter) {
        this.configsBefore = configsBefore;
        this.configsAfter = configsAfter;
    }

    public List<JvmConfigRecordV4Response> getConfigsBefore() {
        return configsBefore;
    }

    public void setConfigsBefore(List<JvmConfigRecordV4Response> configsBefore) {
        this.configsBefore = configsBefore;
    }

    public List<JvmConfigRecordV4Response> getConfigsAfter() {
        return configsAfter;
    }

    public void setConfigsAfter(List<JvmConfigRecordV4Response> configsAfter) {
        this.configsAfter = configsAfter;
    }

    @Override
    public String toString() {
        return "ResetJvmParamsDiffV4Response{" +
                "configsBefore=" + configsBefore +
                ", configsAfter=" + configsAfter +
                '}';
    }
}
