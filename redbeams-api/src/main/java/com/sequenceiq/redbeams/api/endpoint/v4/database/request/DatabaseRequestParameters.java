package com.sequenceiq.redbeams.api.endpoint.v4.database.request;

import static com.sequenceiq.redbeams.doc.ModelDescriptions.Database;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabaseRequestParameters implements Serializable {

    @NotNull
    @ApiModelProperty(value = Database.VERSION, required = true)
    private String version;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
