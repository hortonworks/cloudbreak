package com.sequenceiq.cloudbreak.template.views;

public class EnvironmentView {

    private final String environmentCRN;

    private final String environmentName;

    public EnvironmentView(String environmentCRN, String environmentName) {
        this.environmentCRN = environmentCRN;
        this.environmentName = environmentName;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public String getEnvironmentCRN() {
        return environmentCRN;
    }
}
