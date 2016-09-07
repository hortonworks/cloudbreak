package com.sequenceiq.cloudbreak.api.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class ConstraintTemplateBase implements JsonEntity {

    @Size(max = 100, min = 5, message = "The length of the constraint template's name has to be in range of 5 to 100")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])",
            message = "The name of the constraint template can only contain lowercase characters and hyphens")
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;

    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.ConstraintTemplateModelDescription.CPU, required = true)
    private Double cpu;

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.ConstraintTemplateModelDescription.MEMORY, required = true)
    private Double memory;

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.ConstraintTemplateModelDescription.DISK, required = true)
    private Double disk;

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.ConstraintTemplateModelDescription.ORCHESTRATOR_TYPE, required = true)
    private String orchestratorType;

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

    public Double getCpu() {
        return cpu;
    }

    public void setCpu(Double cpu) {
        this.cpu = cpu;
    }

    public Double getMemory() {
        return memory;
    }

    public void setMemory(Double memory) {
        this.memory = memory;
    }

    public Double getDisk() {
        return disk;
    }

    public void setDisk(Double disk) {
        this.disk = disk;
    }

    public String getOrchestratorType() {
        return orchestratorType;
    }

    public void setOrchestratorType(String orchestratorType) {
        this.orchestratorType = orchestratorType;
    }
}
