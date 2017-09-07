package com.sequenceiq.cloudbreak.api.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.LdapConfigModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class LdapConfigRequest extends LdapConfigBase {

    @Size(max = 100, min = 1, message = "The length of the ldap config's name has to be in range of 1 to 100")
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;

    @NotNull
    @ApiModelProperty(value = LdapConfigModelDescription.BIND_PASSWORD, required = true)
    private String bindPassword;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBindPassword() {
        return bindPassword;
    }

    public void setBindPassword(String bindPassword) {
        this.bindPassword = bindPassword;
    }

}
