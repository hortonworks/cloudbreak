package com.sequenceiq.redbeams.api.endpoint.v4.database.responses;

import com.sequenceiq.redbeams.doc.ModelDescriptions;
import com.sequenceiq.redbeams.doc.ModelDescriptions.Database;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Result of creating a database.
 */
@ApiModel(description = ModelDescriptions.CREATE_DATABASE_RESPONSE)
public class CreateDatabaseV4Response {

    @ApiModelProperty(value = Database.CREATE_RESULT, required = true)
    private String result;

    public CreateDatabaseV4Response() {
        // Do nothing
    }

    public CreateDatabaseV4Response(String createResult) {
        result = createResult;
    }

    /**
     * Gets the result.
     *
     * @return the result
     */
    public String getResult() {
        return result;
    }

    /**
     * Sets the result.
     *
     * @param createResult the result
     */
    public void setResult(String createResult) {
        result = createResult;
    }
}
