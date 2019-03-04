package com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterTemplateModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public abstract class ClusterTemplateV4Base implements JsonEntity {

    @Size(max = 40, min = 5, message = "The length of the cluster's name has to be in range of 5 to 40")
    @Pattern(regexp = "^[^;\\/%]*$",
            message = "The length of the cluster template's name has to be in range of 1 to 100 and should not contain semicolon")
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;

    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @ApiModelProperty(value = ClusterTemplateModelDescription.TEMPLATE, required = true)
    @NotNull
    private StackV4Request stackTemplate;

    @ApiModelProperty(required = true, allowableValues = "SPARK,HIVE,DATASCIENCE,EDW,ETL,OTHER")
    private ClusterTemplateV4Type type = ClusterTemplateV4Type.OTHER;

    @ApiModelProperty(ClusterTemplateModelDescription.CLOUD_PLATFORM)
    private String cloudPlatform;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public StackV4Request getStackTemplate() {
        return stackTemplate;
    }

    public void setStackTemplate(StackV4Request stackTemplate) {
        this.stackTemplate = stackTemplate;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public ClusterTemplateV4Type getType() {
        return type;
    }

    public void setType(ClusterTemplateV4Type type) {
        this.type = type;
    }
}
