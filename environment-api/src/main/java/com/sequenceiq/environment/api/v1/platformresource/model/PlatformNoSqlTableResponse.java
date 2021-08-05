package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformNoSqlTableResponse implements Serializable {

    private String name;

    public PlatformNoSqlTableResponse() {

    }

    public PlatformNoSqlTableResponse(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "PlatformNoSqlTableResponse{" +
                "name='" + name + '\'' +
                '}';
    }
}
