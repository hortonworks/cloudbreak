package com.sequenceiq.freeipa.api.v1.freeipa.user.model;

import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.freeipa.api.v1.freeipa.user.doc.UsersyncModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("UserV1")
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {

    @NotNull
    @ApiModelProperty(value = UsersyncModelDescriptions.USER_NAME, required = true)
    private String name;

    @NotNull
    @ApiModelProperty(value = UsersyncModelDescriptions.USER_FIRSTNAME, required = true)
    private String firstName;

    @NotNull
    @ApiModelProperty(value = UsersyncModelDescriptions.USER_LASTNAME, required = true)
    private String lastName;

    // TODO make this secret
    @ApiModelProperty(value = UsersyncModelDescriptions.USER_PASSWORD)
    private String password;

    @ApiModelProperty(value = UsersyncModelDescriptions.USERSYNC_GROUPS)
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

    public String getPassword() {
        return password;
    }

    public Set<String> getGroups() {
        return groups;
    }
}
