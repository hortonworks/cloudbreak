package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.v2.StorageLocationResponse;
import com.sequenceiq.cloudbreak.domain.StorageLocation;

@Component
public class StorageLocationToStorageLocationResponseConverter extends AbstractConversionServiceAwareConverter<StorageLocation, StorageLocationResponse> {

    @Override
    public StorageLocationResponse convert(StorageLocation source) {
        StorageLocationResponse storageLocation = new StorageLocationResponse();
        storageLocation.setPropertyFile(source.getConfigFile());
        storageLocation.setPropertyName(source.getProperty());
        storageLocation.setValue(source.getValue());
        return storageLocation;
    }
}
