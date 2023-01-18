package com.sequenceiq.environment.api.v1.credential.model.parameters.mock;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MockV1Parameters")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class MockParameters implements Serializable {

    @Schema(hidden = true)
    private String mockEndpoint;

    public String getMockEndpoint() {
        return mockEndpoint;
    }

    public void setMockEndpoint(String mockEndpoint) {
        this.mockEndpoint = mockEndpoint;
    }

    @Override
    public String toString() {
        return "MockParameters{" +
                "mockEndpoint='" + mockEndpoint + '\'' +
                '}';
    }
}
