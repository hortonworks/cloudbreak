package com.sequenceiq.cloudbreak.cloud.gcp;

import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.StorageObject;
import com.sequenceiq.cloudbreak.cloud.ObjectStorageConnector;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateResponse;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import org.springframework.stereotype.Service;

@Service
public class GcpObjectStorageConnector implements ObjectStorageConnector {

    @Override
    public ObjectStorageMetadataResponse getObjectStorageMetadata(ObjectStorageMetadataRequest request) {
        Storage storage = GcpStackUtil.buildStorage(request.getCredential(), request.getCredential().getName());
        try {
            StorageObject storageObject = storage.objects().get(GcpStackUtil.getBucketName(request.getObjectStoragePath()),
                    GcpStackUtil.getPath(request.getObjectStoragePath())).execute();
            return ObjectStorageMetadataResponse.builder()
                    .withStatus(ResponseStatus.OK)
                    .build();
        } catch (Exception e) {
            return ObjectStorageMetadataResponse.builder()
                    .withStatus(ResponseStatus.RESOURCE_NOT_FOUND)
                    .build();
        }
    }

    @Override
    public ObjectStorageValidateResponse validateObjectStorage(ObjectStorageValidateRequest request) {
        // TODO: The following check is naive, beefup
        Storage storage = GcpStackUtil.buildStorage(request.getCredential(), request.getCredential().getName());
        for (StorageLocationBase location : request.getCloudStorageRequest().getLocations()) {
            try {
                StorageObject storageObject = storage.objects().get(GcpStackUtil.getBucketName(location.getValue()),
                        GcpStackUtil.getPath(location.getValue())).execute();
            } catch (Exception e) {
                return ObjectStorageValidateResponse.builder()
                        .withStatus(ResponseStatus.RESOURCE_NOT_FOUND)
                        .withError(location.getValue())
                        .build();
            }
        }
        return ObjectStorageValidateResponse.builder()
                .withStatus(ResponseStatus.OK)
                .build();
    }

    @Override
    public Platform platform() {
        return GcpConstants.GCP_PLATFORM;
    }

    @Override
    public Variant variant() {
        return GcpConstants.GCP_VARIANT;
    }
}
