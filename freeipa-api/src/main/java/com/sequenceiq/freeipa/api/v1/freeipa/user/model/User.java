package com.sequenceiq.freeipa.api.v1.freeipa.user.model;

import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.freeipa.api.v1.freeipa.user.doc.UserModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("UserV1")
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {

    @NotNull
    @ApiModelProperty(value = UserModelDescriptions.USER_NAME, required = true)
    private String name;

    @NotNull
    @ApiModelProperty(value = UserModelDescriptions.USER_FIRSTNAME, required = true)
    private String firstName;

    @NotNull
    @ApiModelProperty(value = UserModelDescriptions.USER_LASTNAME, required = true)
    private String lastName;

    @ApiModelProperty(value = UserModelDescriptions.USER_GROUPS)
    private Set<String> groups;

    public String getName() {
        return name;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Set<String> getGroups() {
        return groups;
    }
}
