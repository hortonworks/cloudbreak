package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.storage.location.StorageLocationV4Response;
import com.sequenceiq.cloudbreak.domain.StorageLocation;

@Component
public class StorageLocationToStorageLocationV4ResponseConverter {

    public StorageLocationV4Response convert(StorageLocation source) {
        StorageLocationV4Response storageLocation = new StorageLocationV4Response();
        storageLocation.setPropertyFile(source.getConfigFile());
        storageLocation.setPropertyName(source.getProperty());
        storageLocation.setValue(source.getValue());
        return storageLocation;
    }
}
