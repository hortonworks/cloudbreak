package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.location.StorageLocationV4Request;
import com.sequenceiq.cloudbreak.domain.StorageLocation;

@Component
public class StorageLocationV4RequestToStorageLocationConverter {

    public StorageLocation convert(StorageLocationV4Request source) {
        StorageLocation storageLocation = new StorageLocation();
        storageLocation.setConfigFile(source.getPropertyFile());
        storageLocation.setProperty(source.getPropertyName());
        storageLocation.setValue(source.getValue());
        return storageLocation;
    }
}
