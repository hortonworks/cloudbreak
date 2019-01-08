package com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("DatabaseV4TestResponse")
@NotNull
public class DatabaseV4TestResponse implements JsonEntity {

    @ApiModelProperty(value = ModelDescriptions.Database.DATABASE_CONNECTION_TEST_RESULT, required = true)
    private String connectionResult;

    public DatabaseV4TestResponse() {

    }

    public DatabaseV4TestResponse(String connectionResult) {
        this.connectionResult = connectionResult;
    }

    public String getConnectionResult() {
        return connectionResult;
    }

    public void setConnectionResult(String connectionResult) {
        this.connectionResult = connectionResult;
    }
}
