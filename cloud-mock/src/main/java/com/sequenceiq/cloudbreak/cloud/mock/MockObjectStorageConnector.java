package com.sequenceiq.cloudbreak.cloud.mock;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.ObjectStorageConnector;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;

@Service
public class MockObjectStorageConnector implements ObjectStorageConnector {

    @Override
    public ObjectStorageMetadataResponse getObjectStorageMetadata(ObjectStorageMetadataRequest request) {
        // for mocking it's necessary to pass the environment's location hence please provide it at the end of the storage request separated by a colon
        // e.g.: someStorageLocationStuff:London
        return ObjectStorageMetadataResponse.builder().withRegion(extractRegionFromStoragePath(request.getObjectStoragePath()).orElse(null)).build();
    }

    @Override
    public Platform platform() {
        return Platform.platform("MOCK");
    }

    @Override
    public Variant variant() {
        return Variant.variant("MOCK");
    }

    private Optional<String> extractRegionFromStoragePath(String path) {
        String[] pathSections = path != null ? path.split(":") : new String[0];
        return pathSections.length == 2 ? Optional.of(pathSections[1]) : Optional.empty();
    }

}
