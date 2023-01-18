package com.sequenceiq.distrox.api.v1.distrox.model.environment;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.ENVIRONMENT_CRN;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.validation.ValidEnvironmentSettings;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@ValidEnvironmentSettings
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DistroXEnvironmentV1Request implements Serializable {

    @Schema(description = ENVIRONMENT_CRN)
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
