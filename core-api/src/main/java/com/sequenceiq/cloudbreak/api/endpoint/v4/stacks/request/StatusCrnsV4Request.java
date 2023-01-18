package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class StatusCrnsV4Request {

    @Schema(description = ModelDescriptions.CRNS)
    private List<String> crns = new ArrayList<>();

    public List<String> getCrns() {
        return crns;
    }

    public void setCrns(List<String> crns) {
        this.crns = crns;
    }
}
