package com.sequenceiq.common.api.diagnostics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListDiagnosticsCollectionResponse implements Serializable {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<DiagnosticsCollection> collections = new ArrayList<>();

    public List<DiagnosticsCollection> getCollections() {
        return collections;
    }

    public void setCollections(List<DiagnosticsCollection> collections) {
        this.collections = collections;
    }
}
