package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.location.StorageLocationV4Request;
import com.sequenceiq.cloudbreak.domain.StorageLocation;

@Component
public class StorageLocationToStorageLocationV4RequestConverter {

    public StorageLocationV4Request convert(StorageLocation source) {
        StorageLocationV4Request storageLocation = new StorageLocationV4Request();
        storageLocation.setPropertyFile(source.getConfigFile());
        storageLocation.setPropertyName(source.getProperty());
        storageLocation.setValue(source.getValue());
        return storageLocation;
    }
}
