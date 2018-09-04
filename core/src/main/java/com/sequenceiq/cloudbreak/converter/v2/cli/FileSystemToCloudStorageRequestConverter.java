package com.sequenceiq.cloudbreak.converter.v2.cli;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.filesystem.AbfsFileSystem;
import com.sequenceiq.cloudbreak.api.model.filesystem.AdlsFileSystem;
import com.sequenceiq.cloudbreak.api.model.filesystem.GcsFileSystem;
import com.sequenceiq.cloudbreak.api.model.filesystem.S3FileSystem;
import com.sequenceiq.cloudbreak.api.model.filesystem.WasbFileSystem;
import com.sequenceiq.cloudbreak.api.model.v2.CloudStorageRequest;
import com.sequenceiq.cloudbreak.api.model.v2.StorageLocationRequest;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.AbfsCloudStorageParameters;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.AdlsCloudStorageParameters;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.GcsCloudStorageParameters;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.S3CloudStorageParameters;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.WasbCloudStorageParameters;
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
            } else if (source.getType().isAbfs()) {
                request.setAbfs(getConversionService().convert(source.getConfigurations().get(AbfsFileSystem.class), AbfsCloudStorageParameters.class));
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
