package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.binduser;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.auth.altus.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("BindUserCreateV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BindUserCreateRequest {
    @NotEmpty
    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @ApiModelProperty(value = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    private String environmentCrn;

    @NotEmpty
    @ApiModelProperty(value = "Bind user suffix, eg cluster name", required = true)
    private String bindUserNameSuffix;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getBindUserNameSuffix() {
        return bindUserNameSuffix;
    }

    public void setBindUserNameSuffix(String bindUserNameSuffix) {
        this.bindUserNameSuffix = bindUserNameSuffix;
    }

    @Override
    public String toString() {
        return "BindUserCreateRequest{" +
                "environmentCrn='" + environmentCrn + '\'' +
                ", bindUserNameSuffix='" + bindUserNameSuffix + '\'' +
                '}';
    }
}
