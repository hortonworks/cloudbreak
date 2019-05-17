package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses;

import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServer;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

@ApiModel
@NotNull
public class DatabaseServerTestV4Response implements Serializable {

    @ApiModelProperty(value = DatabaseServer.DATABASE_SERVER_CONNECTION_TEST_RESULT, required = true)
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
