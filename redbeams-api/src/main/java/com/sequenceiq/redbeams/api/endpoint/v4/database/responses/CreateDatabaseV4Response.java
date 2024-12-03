package com.sequenceiq.redbeams.api.endpoint.v4.database.responses;

import com.sequenceiq.redbeams.doc.ModelDescriptions;
import com.sequenceiq.redbeams.doc.ModelDescriptions.Database;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Result of creating a database.
 */
@Schema(description = ModelDescriptions.CREATE_DATABASE_RESPONSE)
public class CreateDatabaseV4Response {

    @Schema(description = Database.CREATE_RESULT, requiredMode = Schema.RequiredMode.REQUIRED)
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
