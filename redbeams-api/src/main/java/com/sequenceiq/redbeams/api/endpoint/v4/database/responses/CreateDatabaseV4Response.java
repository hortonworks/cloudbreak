package com.sequenceiq.redbeams.api.endpoint.v4.database.responses;

import static com.sequenceiq.redbeams.doc.ModelDescriptions.Database;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Result of creating a database.
 */
@ApiModel
public class CreateDatabaseV4Response {

    @ApiModelProperty(value = Database.DATABASE_CREATE_RESULT, required = true)
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
