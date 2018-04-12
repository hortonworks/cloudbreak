package com.sequenceiq.cloudbreak.api.model.mpack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ManagementPackDetails extends ManagementPackBase {
    @ApiModelProperty(value = ModelDescriptions.MpackDetailsDescription.STACK_DEFAULT)
    private boolean stackDefault;

    @ApiModelProperty(value = ModelDescriptions.MpackDetailsDescription.PREINSTALLED)
    private boolean preInstalled;

    public boolean isStackDefault() {
        return stackDefault;
    }

    public void setStackDefault(boolean stackDefault) {
        this.stackDefault = stackDefault;
    }

    public boolean isPreInstalled() {
        return preInstalled;
    }

    public void setPreInstalled(boolean preInstalled) {
        this.preInstalled = preInstalled;
    }
}
