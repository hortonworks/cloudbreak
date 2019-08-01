package com.sequenceiq.cloudbreak.converter.v4.stacks.cli;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.location.StorageLocationV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.domain.StorageLocations;
import com.sequenceiq.common.api.cloudstorage.AdlsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.WasbCloudStorageV1Parameters;
import com.sequenceiq.common.api.filesystem.AdlsFileSystem;
import com.sequenceiq.common.api.filesystem.AdlsGen2FileSystem;
import com.sequenceiq.common.api.filesystem.GcsFileSystem;
import com.sequenceiq.common.api.filesystem.S3FileSystem;
import com.sequenceiq.common.api.filesystem.WasbFileSystem;

@Component
public class FileSystemToCloudStorageV4RequestConverter extends AbstractConversionServiceAwareConverter<FileSystem, CloudStorageV4Request> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemToCloudStorageV4RequestConverter.class);

    @Override
    public CloudStorageV4Request convert(FileSystem source) {
        CloudStorageV4Request request = new CloudStorageV4Request();
        request.setLocations(getStorageLocationRequests(source));
        try {
            if (source.getConfigurations() != null && source.getConfigurations().getValue() != null) {
                if (source.getType().isAdls()) {
                    request.setAdls(getConversionService().convert(source.getConfigurations().get(AdlsFileSystem.class), AdlsCloudStorageV1Parameters.class));
                } else if (source.getType().isGcs()) {
                    request.setGcs(getConversionService().convert(source.getConfigurations().get(GcsFileSystem.class), GcsCloudStorageV1Parameters.class));
                } else if (source.getType().isS3()) {
                    request.setS3(getConversionService().convert(source.getConfigurations().get(S3FileSystem.class), S3CloudStorageV1Parameters.class));
                } else if (source.getType().isWasb()) {
                    request.setWasb(getConversionService().convert(source.getConfigurations().get(WasbFileSystem.class), WasbCloudStorageV1Parameters.class));
                } else if (source.getType().isAdlsGen2()) {
                    request.setAdlsGen2(getConversionService().convert(source.getConfigurations().get(AdlsGen2FileSystem.class),
                            AdlsGen2CloudStorageV1Parameters.class));
                }
            }
        } catch (IOException ioe) {
            LOGGER.info("Something happened while we tried to obtain/convert file system", ioe);
        }
        return request;
    }

    private Set<StorageLocationV4Request> getStorageLocationRequests(FileSystem source) {
        Set<StorageLocationV4Request> locations = new HashSet<>();
        try {
            if (source.getLocations() != null && source.getLocations().getValue() != null) {
                StorageLocations storageLocations = source.getLocations().get(StorageLocations.class);
                if (storageLocations != null) {
                    for (StorageLocation storageLocation : storageLocations.getLocations()) {
                        locations.add(getConversionService().convert(storageLocation, StorageLocationV4Request.class));
                    }
                }
            } else {
                locations = new HashSet<>();
            }
        } catch (IOException e) {
            locations = new HashSet<>();
        }
        return locations;
    }

}
