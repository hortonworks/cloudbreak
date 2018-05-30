package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.v2.StorageLocationRequest;
import com.sequenceiq.cloudbreak.domain.StorageLocation;

@Component
public class StorageLocationToStorageLocationReqestConverter extends AbstractConversionServiceAwareConverter<StorageLocation, StorageLocationRequest> {

    @Override
    public StorageLocationRequest convert(StorageLocation source) {
        StorageLocationRequest storageLocation = new StorageLocationRequest();
        storageLocation.setPropertyFile(source.getConfigFile());
        storageLocation.setPropertyName(source.getProperty());
        storageLocation.setValue(source.getValue());
        return storageLocation;
    }
}
