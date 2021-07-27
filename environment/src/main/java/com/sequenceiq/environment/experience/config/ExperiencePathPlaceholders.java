package com.sequenceiq.environment.experience.config;

public enum ExperiencePathPlaceholders {

    ENVIRONMENT_CRN("envCrn"),
    CLOUD_PROVIDER("cloudProvider");

    private final String placeholder;

    ExperiencePathPlaceholders(String placeholder) {
        this.placeholder = placeholder;
    }

    public String getPlaceholder() {
        return placeholder;
    }

}
