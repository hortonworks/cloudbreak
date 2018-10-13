package com.sequenceiq.cloudbreak.api.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.PlatformResourceRequestModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformResourceRequestJson {

    @ApiModelProperty(PlatformResourceRequestModelDescription.CREDENTIAL_ID)
    private Long credentialId;

    @ApiModelProperty(PlatformResourceRequestModelDescription.CREDENTIAL_NAME)
    private String credentialName;

    @ApiModelProperty(PlatformResourceRequestModelDescription.REGION)
    private String region;

    @ApiModelProperty(StackModelDescription.PLATFORM_VARIANT)
    private String platformVariant;

    @ApiModelProperty(PlatformResourceRequestModelDescription.FILTER)
    private Map<String, String> filters = new HashMap<>();

    @ApiModelProperty(PlatformResourceRequestModelDescription.AVAILABILITY_ZONE)
    private String availabilityZone;

    public Long getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(Long credentialId) {
        this.credentialId = credentialId;
    }

    public String getCredentialName() {
        return credentialName;
    }

    public void setCredentialName(String credentialName) {
        this.credentialName = credentialName;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getPlatformVariant() {
        return platformVariant;
    }

    public void setPlatformVariant(String platformVariant) {
        this.platformVariant = platformVariant;
    }

    public Map<String, String> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, String> filters) {
        this.filters = filters;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }
}
