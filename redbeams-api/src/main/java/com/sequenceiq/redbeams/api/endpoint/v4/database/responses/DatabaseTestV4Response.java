package com.sequenceiq.redbeams.api.endpoint.v4.database.responses;

import static com.sequenceiq.redbeams.doc.ModelDescriptions.Database;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@NotNull
public class DatabaseTestV4Response implements Serializable {

    @ApiModelProperty(value = Database.DATABASE_CONNECTION_TEST_RESULT, required = true)
    private String result;

    public DatabaseTestV4Response() {

    }

    public DatabaseTestV4Response(String connectionResult) {
        result = connectionResult;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
