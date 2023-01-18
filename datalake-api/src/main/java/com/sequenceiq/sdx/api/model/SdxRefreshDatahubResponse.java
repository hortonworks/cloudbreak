package com.sequenceiq.sdx.api.model;

import static com.sequenceiq.sdx.api.model.ModelDescriptions.DATAHUB_CRNS_REFRESHED;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxRefreshDatahubResponse {

    @Schema(description = DATAHUB_CRNS_REFRESHED)
    private List<StackViewV4Response> refreshedDatahubs;

    public SdxRefreshDatahubResponse() {
        this.refreshedDatahubs = new ArrayList<>();
    }

    public SdxRefreshDatahubResponse(List<StackViewV4Response> refreshedDatahubs) {
        this.refreshedDatahubs = refreshedDatahubs;
    }

    public List<StackViewV4Response> getRefreshedDatahubs() {
        return refreshedDatahubs;
    }

    public void setRefreshedDatahubs(List<StackViewV4Response> refreshedDatahubs) {
        this.refreshedDatahubs = refreshedDatahubs;
    }

    public void addStacks(List<StackViewV4Response> stacks) {
        this.refreshedDatahubs.addAll(stacks);
    }

    public void addStack(StackV4Response stack) {
        StackViewV4Response response = new StackViewV4Response();
        response.setCrn(stack.getCrn());
        response.setName(stack.getName());
        response.setEnvironmentName(stack.getEnvironmentName());
        response.setEnvironmentCrn(stack.getEnvironmentCrn());
        addStack(response);
    }

    public void addStack(StackViewV4Response stack) {
        this.refreshedDatahubs.add(stack);
    }

    @Override
    public String toString() {
        return "SdxRefreshDatahubResponse{" + "refreshedDatahubs=" + refreshedDatahubs + '}';
    }
}
