package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Map;

public class SpecialParameters {

    private Map<String, Boolean> specialParameters;

    public SpecialParameters(Map<String, Boolean> specialParameters) {
        this.specialParameters = specialParameters;
    }

    public Map<String, Boolean> getSpecialParameters() {
        return specialParameters;
    }

    public void setSpecialParameters(Map<String, Boolean> specialParameters) {
        this.specialParameters = specialParameters;
    }
}
