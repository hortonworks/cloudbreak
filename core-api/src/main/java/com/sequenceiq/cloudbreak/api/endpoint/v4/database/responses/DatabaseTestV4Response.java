package com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@NotNull
public class DatabaseTestV4Response implements JsonEntity {

    @ApiModelProperty(value = ModelDescriptions.Database.DATABASE_CONNECTION_TEST_RESULT, required = true)
    private String result;

    public DatabaseTestV4Response() {

    }

    public DatabaseTestV4Response(String connectionResult) {
        this.result = connectionResult;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
