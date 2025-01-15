package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses;

import static com.sequenceiq.redbeams.doc.ModelDescriptions.AZURE_DATABASE_PROPERTIES_RESPONSE;
import static com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServer;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = AZURE_DATABASE_PROPERTIES_RESPONSE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureDatabasePropertiesV4Response {

    @Schema(description = DatabaseServer.RESOURCE_GROUP_NAME)
    private String resourceGroup;

    @Schema(description = DatabaseServer.RESOURCE_ID)
    private String resourceId;

    public String getResourceGroup() {
        return resourceGroup;
    }

    public void setResourceGroup(String resourceGroup) {
        this.resourceGroup = resourceGroup;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AzureDatabasePropertiesV4Response.class.getSimpleName() + "[", "]")
                .add("resourceGroup='" + resourceGroup + "'")
                .add("resourceId='" + resourceId + "'")
                .toString();
    }
}
