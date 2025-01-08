package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.redbeams.doc.ModelDescriptions;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServer;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.DATABASE_PROPERTIES_RESPONSE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabasePropertiesV4Response {
    @Schema(description = DatabaseServer.CONNECTION_NAME_FORMAT, required = true)
    private ConnectionNameFormat connectionNameFormat = ConnectionNameFormat.USERNAME_ONLY;

    public ConnectionNameFormat getConnectionNameFormat() {
        return connectionNameFormat == null ? ConnectionNameFormat.USERNAME_ONLY : connectionNameFormat;
    }

    public void setConnectionNameFormat(ConnectionNameFormat connectionNameFormat) {
        this.connectionNameFormat = connectionNameFormat;
    }
}
