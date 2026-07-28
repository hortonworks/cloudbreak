package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.util.OneOfEnum;
import com.sequenceiq.common.api.type.OrchestratorType;
import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.FreeIpaVerticalScalingModelDescriptions;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class VerticalScaleRequest implements JsonEntity {

    @NotNull
    private String group;

    @Valid
    @NotNull
    private InstanceTemplateRequest template;

    @Schema(description = FreeIpaVerticalScalingModelDescriptions.ORCHESTRATOR, defaultValue = "ALL_AT_ONCE",
            allowableValues = {"ALL_AT_ONCE", "ONE_BY_ONE"})
    @OneOfEnum(enumClass = OrchestratorType.class, message = "orchestratorType must be one of %s", fieldName = "orchestratorType")
    private String orchestratorType = OrchestratorType.ALL_AT_ONCE.name();

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public InstanceTemplateRequest getTemplate() {
        return template;
    }

    public void setTemplate(InstanceTemplateRequest template) {
        this.template = template;
    }

    public String getOrchestratorType() {
        return orchestratorType;
    }

    public void setOrchestratorType(String orchestratorType) {
        this.orchestratorType = orchestratorType;
    }

    @Override
    public String toString() {
        return "VerticalScaleRequest{" +
                "group='" + group + '\'' +
                ", template=" + template +
                ", orchestratorType='" + orchestratorType + '\'' +
                '}';
    }
}
