package com.sequenceiq.cloudbreak.domain;

import java.util.ArrayList;
import java.util.List;

public class BlueprintInputParameters {

    private List<BlueprintParameter> parameters = new ArrayList<>();

    public BlueprintInputParameters() { }

    public BlueprintInputParameters(List<BlueprintParameter> parameters) {
        this.parameters = parameters;
    }

    public List<BlueprintParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<BlueprintParameter> parameters) {
        this.parameters = parameters;
    }
}
