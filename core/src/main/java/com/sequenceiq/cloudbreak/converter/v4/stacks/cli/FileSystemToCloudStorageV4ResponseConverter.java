package com.sequenceiq.cloudbreak.converter.v4.stacks.cli;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.AdlsCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.AdlsGen2CloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.GcsCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.S3CloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.WasbCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.storage.CloudStorageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.storage.location.StorageLocationV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.domain.StorageLocations;
import com.sequenceiq.cloudbreak.services.filesystem.AdlsFileSystem;
import com.sequenceiq.cloudbreak.services.filesystem.AdlsGen2FileSystem;
import com.sequenceiq.cloudbreak.services.filesystem.GcsFileSystem;
import com.sequenceiq.cloudbreak.services.filesystem.S3FileSystem;
import com.sequenceiq.cloudbreak.services.filesystem.WasbFileSystem;

@Component
public class FileSystemToCloudStorageV4ResponseConverter extends AbstractConversionServiceAwareConverter<FileSystem, CloudStorageV4Response> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemToCloudStorageV4ResponseConverter.class);

    @Override
    public CloudStorageV4Response convert(FileSystem source) {
        CloudStorageV4Response response = new CloudStorageV4Response();
        response.setId(source.getId());
        response.setName(source.getName());
        response.setDefaultFs(source.isDefaultFs());
        response.setLocations(getStorageLocationRequests(source));
        try {
            if (source.getType().isAdls()) {
                AdlsCloudStorageV4Parameters adls = getConversionService()
                        .convert(source.getConfigurations().get(AdlsFileSystem.class), AdlsCloudStorageV4Parameters.class);
                adls.setCredential(null);
                response.setAdls(adls);
            } else if (source.getType().isGcs()) {
                response.setGcs(getConversionService().convert(source.getConfigurations().get(GcsFileSystem.class), GcsCloudStorageV4Parameters.class));
            } else if (source.getType().isS3()) {
                response.setS3(getConversionService().convert(source.getConfigurations().get(S3FileSystem.class), S3CloudStorageV4Parameters.class));
            } else if (source.getType().isWasb()) {
                response.setWasb(getConversionService().convert(source.getConfigurations().get(WasbFileSystem.class), WasbCloudStorageV4Parameters.class));
            } else if (source.getType().isAdlsGen2()) {
                response.setAdlsGen2(getConversionService().convert(source.getConfigurations().get(AdlsGen2FileSystem.class),
                        AdlsGen2CloudStorageV4Parameters.class));
            }
        } catch (IOException ioe) {
            LOGGER.info("Something happened while we tried to obtain/convert file system", ioe);
        }
        response.setType(source.getType().name());
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
