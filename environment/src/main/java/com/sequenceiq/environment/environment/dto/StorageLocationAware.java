package com.sequenceiq.environment.environment.dto;

import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.environment.environment.dto.telemetry.S3CloudStorageParameters;

public interface StorageLocationAware {

    S3CloudStorageParameters getS3();

    GcsCloudStorageV1Parameters getGcs();

    AdlsGen2CloudStorageV1Parameters getAdlsGen2();

    void setStorageLocation(String storageLocation);

}
