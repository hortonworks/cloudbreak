package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@NotNull
public class RuntimeVersionsV4Response {

    private List<String> runtimeVersions;

    @JsonCreator
    public RuntimeVersionsV4Response(@JsonProperty("runtimeVersions") List<String> runtimeVersions) {
        this.runtimeVersions = runtimeVersions;
    }

    @Override
    public String toString() {
        return "RuntimeVersionsV4Response{" +
                "runtimeVersions=" + runtimeVersions +
                '}';
    }

    public List<String> getRuntimeVersions() {
        return runtimeVersions;
    }
}
