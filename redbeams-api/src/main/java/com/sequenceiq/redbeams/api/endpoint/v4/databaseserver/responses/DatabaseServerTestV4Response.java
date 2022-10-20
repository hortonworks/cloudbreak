package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.sequenceiq.redbeams.doc.ModelDescriptions;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServerTest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = ModelDescriptions.DATABASE_SERVER_TEST_RESPONSE)
@NotNull
public class DatabaseServerTestV4Response implements Serializable {

    @ApiModelProperty(value = DatabaseServerTest.RESULT, required = true)
    private String result;

    public DatabaseServerTestV4Response() {
    }

    public DatabaseServerTestV4Response(String connectionResult) {
        result = connectionResult;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

}
