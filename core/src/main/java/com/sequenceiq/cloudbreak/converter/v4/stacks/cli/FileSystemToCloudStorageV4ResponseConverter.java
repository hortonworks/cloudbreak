package com.sequenceiq.cloudbreak.converter.v4.stacks.cli;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.storage.CloudStorageV1Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.storage.location.StorageLocationV4Response;
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
public class FileSystemToCloudStorageV4ResponseConverter extends AbstractConversionServiceAwareConverter<FileSystem, CloudStorageV1Response> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemToCloudStorageV4ResponseConverter.class);

    @Override
    public CloudStorageV1Response convert(FileSystem source) {
        CloudStorageV1Response response = new CloudStorageV1Response();
        response.setId(source.getId());
        response.setName(source.getName());
        response.setLocations(getStorageLocationRequests(source));
        if (source.getType() != null) {
            response.setType(source.getType().name());
            try {
                if (source.getConfigurations() != null && source.getConfigurations().getValue() != null) {
                    if (source.getType().isAdls()) {
                        AdlsCloudStorageV1Parameters adls = getConversionService()
                                .convert(source.getConfigurations().get(AdlsFileSystem.class), AdlsCloudStorageV1Parameters.class);
                        adls.setCredential(null);
                        response.setAdls(adls);
                    } else if (source.getType().isGcs()) {
                        response.setGcs(getConversionService().convert(source.getConfigurations()
                                .get(GcsFileSystem.class), GcsCloudStorageV1Parameters.class));
                    } else if (source.getType().isS3()) {
                        response.setS3(getConversionService().convert(source.getConfigurations().get(S3FileSystem.class), S3CloudStorageV1Parameters.class));
                    } else if (source.getType().isWasb()) {
                        response.setWasb(getConversionService().convert(source.getConfigurations()
                                .get(WasbFileSystem.class), WasbCloudStorageV1Parameters.class));
                    } else if (source.getType().isAdlsGen2()) {
                        response.setAdlsGen2(getConversionService().convert(source.getConfigurations().get(AdlsGen2FileSystem.class),
                                AdlsGen2CloudStorageV1Parameters.class));
                    }
                }
            } catch (IOException ioe) {
                LOGGER.info("Something happened while we tried to obtain/convert file system", ioe);
            }
        }
        return response;
    }

    private Set<StorageLocationV4Response> getStorageLocationRequests(FileSystem source) {
        Set<StorageLocationV4Response> locations = new HashSet<>();
        try {
            if (source.getLocations() != null && source.getLocations().getValue() != null) {
                StorageLocations storageLocations = source.getLocations().get(StorageLocations.class);
                if (storageLocations != null) {
                    for (StorageLocation storageLocation : storageLocations.getLocations()) {
                        locations.add(getConversionService().convert(storageLocation, StorageLocationV4Response.class));
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
