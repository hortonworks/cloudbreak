package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses;

import com.sequenceiq.redbeams.doc.ModelDescriptions;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServer;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.STORAGE_PROPERTIES_RESPONSE)
public class StoragePropertiesV4Response {

    @Schema(description = DatabaseServer.LOW_STORAGE)
    private boolean lowStorage;

    public boolean isLowStorage() {
        return lowStorage;
    }

    public void setLowStorage(boolean lowStorage) {
        this.lowStorage = lowStorage;
    }

    @Override
    public String toString() {
        return "StoragePropertiesV4Response{" +
                "lowStorage=" + lowStorage +
                '}';
    }
}
