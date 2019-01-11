package com.sequenceiq.cloudbreak.converter.v2.cli;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.adls.AdlsGen2FileSystem;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.adls.AdlsFileSystem;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.gcs.GcsFileSystem;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.s3.S3FileSystem;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.wasb.WasbFileSystem;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.CloudStorageRequest;
import com.sequenceiq.cloudbreak.api.model.v2.StorageLocationRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.adls.AdlsGen2CloudStorageParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.adls.AdlsCloudStorageParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.gcs.GcsCloudStorageParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.s3.S3CloudStorageParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.wasb.WasbCloudStorageParameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.domain.StorageLocations;

@Component
public class FileSystemToCloudStorageRequestConverter extends AbstractConversionServiceAwareConverter<FileSystem, CloudStorageRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemToCloudStorageRequestConverter.class);

    @Override
    public CloudStorageRequest convert(FileSystem source) {
        CloudStorageRequest request = new CloudStorageRequest();
        request.setLocations(getStorageLocationRequests(source));
        try {
            if (source.getType().isAdls()) {
                request.setAdls(getConversionService().convert(source.getConfigurations().get(AdlsFileSystem.class), AdlsCloudStorageParameters.class));
            } else if (source.getType().isGcs()) {
                request.setGcs(getConversionService().convert(source.getConfigurations().get(GcsFileSystem.class), GcsCloudStorageParameters.class));
            } else if (source.getType().isS3()) {
                request.setS3(getConversionService().convert(source.getConfigurations().get(S3FileSystem.class), S3CloudStorageParameters.class));
            } else if (source.getType().isWasb()) {
                request.setWasb(getConversionService().convert(source.getConfigurations().get(WasbFileSystem.class), WasbCloudStorageParameters.class));
            } else if (source.getType().isAdlsGen2()) {
                request.setAdlsGen2(getConversionService().convert(source.getConfigurations().get(AdlsGen2FileSystem.class),
                        AdlsGen2CloudStorageParameters.class));
            }
        } catch (IOException ioe) {
            LOGGER.warn("Something happened while we tried to obtain/convert file system", ioe);
        }
        return request;
    }

    private Set<StorageLocationRequest> getStorageLocationRequests(FileSystem source) {
        Set<StorageLocationRequest> locations = new HashSet<>();
        try {
            if (source.getLocations() != null && source.getLocations().getValue() != null) {
                StorageLocations storageLocations = source.getLocations().get(StorageLocations.class);
                if (storageLocations != null) {
                    for (StorageLocation storageLocationRequest : storageLocations.getLocations()) {
                        locations.add(getConversionService().convert(storageLocationRequest, StorageLocationRequest.class));
                    }
                }
            } else {
                locations = new HashSet<>();
            }
        } catch (IOException ignored) {
            locations = new HashSet<>();
        }
        return locations;
    }

}
