package com.sequenceiq.cloudbreak.api.model.v2;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.stack.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StackFromTemplateRequest implements JsonEntity {
    @Valid
    @NotNull
    @ApiModelProperty(ModelDescriptions.StackModelDescription.GENERAL_SETTINGS)
    private GeneralSettings general;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.AUTHENTICATION)
    @Valid
    @NotNull
    private StackAuthenticationRequest stackAuthentication;

    @NotNull
    @Size(max = 100, min = 5, message = "The length of the password has to be in range of 5 to 100")
    @ApiModelProperty(value = ModelDescriptions.StackModelDescription.PASSWORD, required = true)
    private String ambariPassword;

    @Size(max = 15, min = 5, message = "The length of the username has to be in range of 5 to 15")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The username can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.StackModelDescription.USERNAME, required = true)
    private String ambariUserName;

    public GeneralSettings getGeneral() {
        return general;
    }

    public void setGeneral(GeneralSettings general) {
        this.general = general;
    }

    public StackAuthenticationRequest getStackAuthentication() {
        return stackAuthentication;
    }

    public void setStackAuthentication(StackAuthenticationRequest stackAuthentication) {
        this.stackAuthentication = stackAuthentication;
    }

    public String getAmbariPassword() {
        return ambariPassword;
    }

    public void setAmbariPassword(String ambariPassword) {
        this.ambariPassword = ambariPassword;
    }

    public String getAmbariUserName() {
        return ambariUserName;
    }

    public void setAmbariUserName(String ambariUserName) {
        this.ambariUserName = ambariUserName;
    }
}
