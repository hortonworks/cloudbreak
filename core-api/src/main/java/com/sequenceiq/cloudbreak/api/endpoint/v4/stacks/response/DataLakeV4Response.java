package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DataLakeV4Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DataLakeV4Response(String name, String crn, String platform) {

    @Override
    public String toString() {
        return "DataLakeV4Response{" +
                "name='" + name + '\'' +
                ", crn='" + crn + '\'' +
                ", platform='" + platform + '\'' +
                '}';
    }
}
