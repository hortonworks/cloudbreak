package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class CredentialViewV4Response implements JsonEntity {
    @Size(max = 100, min = 5, message = "The length of the credential's name has to be in range of 5 to 100")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The name of the credential can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.CLOUD_PLATFORM, required = true)
    private String cloudPlatform;

    @ApiModelProperty(ModelDescriptions.GOV_CLOUD_FLAG)
    private Boolean govCloud;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public Boolean getGovCloud() {
        return govCloud;
    }

    public void setGovCloud(Boolean govCloud) {
        this.govCloud = govCloud;
    }

    @Override
    public String toString() {
        return "CredentialBase{"
                + "name='" + name + '\''
                + ", cloudPlatform='" + cloudPlatform + '\''
                + '}';
    }
}
