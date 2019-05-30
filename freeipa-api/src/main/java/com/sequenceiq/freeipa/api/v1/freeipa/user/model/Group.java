package com.sequenceiq.freeipa.api.v1.freeipa.user.model;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.freeipa.api.v1.freeipa.user.doc.UserModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("GroupV1")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Group {

    @NotNull
    @ApiModelProperty(value = UserModelDescriptions.GROUP_NAME, required = true)
    private String name;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Group other = (Group) o;

        return Objects.equals(this.name, other.name);

    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Group{"
                + "name='" + name + '\''
                + '}';
    }
}