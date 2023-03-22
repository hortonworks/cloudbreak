package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses;

import com.sequenceiq.redbeams.doc.ModelDescriptions;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServer;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = ModelDescriptions.DATABASE_PROPERTIES_RESPONSE)
public class DatabasePropertiesV4Response {
    @ApiModelProperty(value = DatabaseServer.CONNECTION_NAME_FORMAT, required = true)
    private ConnectionNameFormat connectionNameFormat = ConnectionNameFormat.USERNAME_ONLY;

    public ConnectionNameFormat getConnectionNameFormat() {
        return connectionNameFormat == null ? ConnectionNameFormat.USERNAME_ONLY : connectionNameFormat;
    }

    public void setConnectionNameFormat(ConnectionNameFormat connectionNameFormat) {
        this.connectionNameFormat = connectionNameFormat;
    }
}
