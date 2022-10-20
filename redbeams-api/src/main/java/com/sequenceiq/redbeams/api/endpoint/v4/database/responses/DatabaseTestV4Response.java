package com.sequenceiq.redbeams.api.endpoint.v4.database.responses;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.sequenceiq.redbeams.doc.ModelDescriptions;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseTest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = ModelDescriptions.DATABASE_TEST_RESPONSE)
@NotNull
public class DatabaseTestV4Response implements Serializable {

    @ApiModelProperty(value = DatabaseTest.RESULT, required = true)
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
