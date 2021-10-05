package com.sequenceiq.sdx.api.model;

import java.io.Serializable;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SdxRecipe implements Serializable {

    @NotBlank
    private String name;

    @NotBlank
    private String hostGroup;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHostGroup() {
        return hostGroup;
    }

    public void setHostGroup(String hostGroup) {
        this.hostGroup = hostGroup;
    }

    @Override
    public String toString() {
        return "SdxRecipe{" +
                "name='" + name + '\'' +
                ", hostGroup='" + hostGroup + '\'' +
                '}';
    }
}
