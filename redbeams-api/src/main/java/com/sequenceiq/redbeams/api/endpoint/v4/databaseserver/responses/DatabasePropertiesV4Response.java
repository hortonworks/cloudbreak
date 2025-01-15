package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.redbeams.doc.ModelDescriptions;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServer;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.DATABASE_PROPERTIES_RESPONSE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabasePropertiesV4Response {

    @Schema(description = DatabaseServer.CONNECTION_NAME_FORMAT, requiredMode = Schema.RequiredMode.REQUIRED)
    private ConnectionNameFormat connectionNameFormat = ConnectionNameFormat.USERNAME_ONLY;

    private String databaseType;

    private AzureDatabasePropertiesV4Response azure;

    public ConnectionNameFormat getConnectionNameFormat() {
        return connectionNameFormat == null ? ConnectionNameFormat.USERNAME_ONLY : connectionNameFormat;
    }

    public void setConnectionNameFormat(ConnectionNameFormat connectionNameFormat) {
        this.connectionNameFormat = connectionNameFormat;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }

    public AzureDatabasePropertiesV4Response getAzure() {
        return azure;
    }

    public void setAzure(AzureDatabasePropertiesV4Response azure) {
        this.azure = azure;
    }

    @Override
    public String toString() {
        return "DatabasePropertiesV4Response{" +
                "connectionNameFormat=" + connectionNameFormat +
                ", databaseType='" + databaseType + '\'' +
                ", azure='" + azure + '\'' +
                '}';
    }
}