package com.sequenceiq.cloudbreak.api.model;

import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformVariantsJson implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.ConnectorModelDescription.PLATFORM_VARIANTS)
    private Map<String, Collection<String>> platformToVariants;

    @ApiModelProperty(ModelDescriptions.ConnectorModelDescription.DEFAULT_VARIANTS)
    private Map<String, String> defaultVariants;

    public Map<String, Collection<String>> getPlatformToVariants() {
        return platformToVariants;
    }

    public void setPlatformToVariants(Map<String, Collection<String>> platformToVariants) {
        this.platformToVariants = platformToVariants;
    }

    public Map<String, String> getDefaultVariants() {
        return defaultVariants;
    }

    public void setDefaultVariants(Map<String, String> defaultVariants) {
        this.defaultVariants = defaultVariants;
    }
}
