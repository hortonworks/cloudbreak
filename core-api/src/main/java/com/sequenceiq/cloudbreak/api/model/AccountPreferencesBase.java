package com.sequenceiq.cloudbreak.api.model;

import java.util.Map;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.AccountPreferencesModelDescription;

import io.swagger.annotations.ApiModelProperty;

public abstract class AccountPreferencesBase implements JsonEntity {

    @ApiModelProperty(AccountPreferencesModelDescription.PLATFORMS)
    private String platforms;

    @ApiModelProperty(AccountPreferencesModelDescription.SMARTSENSE_ENABLED)
    private boolean smartsenseEnabled;

    @ApiModelProperty(AccountPreferencesModelDescription.DEFAULT_TAGS)
    private Map<String, String> defaultTags;

    public String getPlatforms() {
        return platforms;
    }

    public void setPlatforms(String platforms) {
        this.platforms = platforms;
    }

    public Map<String, String> getDefaultTags() {
        return defaultTags;
    }

    public void setDefaultTags(Map<String, String> defaultTags) {
        this.defaultTags = defaultTags;
    }

    public boolean isSmartsenseEnabled() {
        return smartsenseEnabled;
    }

    public void setSmartsenseEnabled(boolean smartsenseEnabled) {
        this.smartsenseEnabled = smartsenseEnabled;
    }
}
