package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.DatabaseServerModelDescription;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response for increasing the storage size of the database server of a cluster")
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabaseServerIncreaseStorageSizeV4Response {

    @Schema(description = DatabaseServerModelDescription.FLOW_IDENTIFIER)
    private FlowIdentifier flowIdentifier;

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setFlowIdentifier(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    @Override
    public String toString() {
        return "DatabaseServerIncreaseStorageSizeV4Response{" +
                "flowIdentifier=" + flowIdentifier +
                '}';
    }
}
