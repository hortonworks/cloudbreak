package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DatabaseVmTypeResponse implements Serializable {

    @Schema
    private String value;

    public DatabaseVmTypeResponse() {

    }

    public DatabaseVmTypeResponse(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "VmTypeResponse{" +
                "value='" + value + '\'' +
                '}';
    }
}
