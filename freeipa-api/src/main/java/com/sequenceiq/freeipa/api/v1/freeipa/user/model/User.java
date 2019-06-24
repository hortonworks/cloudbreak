package com.sequenceiq.freeipa.api.v1.freeipa.user.model;

import java.util.Objects;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        User other = (User) o;
        return Objects.equals(this.name, other.name)
                && Objects.equals(this.firstName, other.firstName)
                && Objects.equals(this.lastName, other.lastName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, firstName, lastName);
    }

    @Override
    public String toString() {
        return "User{"
                + "name='" + name + '\''
                + ", firstName='" + firstName + '\''
                + ", lastName='" + lastName + '\''
                + '}';
    }
}
