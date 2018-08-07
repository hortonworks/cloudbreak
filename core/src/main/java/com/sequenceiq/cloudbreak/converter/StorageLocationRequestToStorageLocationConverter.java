package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.v2.StorageLocationRequest;
import com.sequenceiq.cloudbreak.domain.StorageLocation;

@Component
public class StorageLocationRequestToStorageLocationConverter extends AbstractConversionServiceAwareConverter<StorageLocationRequest, StorageLocation> {

    @Override
    public StorageLocation convert(StorageLocationRequest source) {
        StorageLocation storageLocation = new StorageLocation();
        storageLocation.setConfigFile(source.getPropertyFile());
        storageLocation.setProperty(source.getPropertyName());
        storageLocation.setValue(source.getValue());
        return storageLocation;
    }
}
