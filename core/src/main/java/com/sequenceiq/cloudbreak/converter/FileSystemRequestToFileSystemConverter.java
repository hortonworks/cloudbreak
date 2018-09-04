package com.sequenceiq.cloudbreak.converter;

import static com.sequenceiq.cloudbreak.common.type.APIResourceType.FILESYSTEM;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.FileSystemRequest;
import com.sequenceiq.cloudbreak.api.model.filesystem.AbfsFileSystem;
import com.sequenceiq.cloudbreak.api.model.filesystem.AdlsFileSystem;
import com.sequenceiq.cloudbreak.api.model.filesystem.BaseFileSystem;
import com.sequenceiq.cloudbreak.api.model.filesystem.GcsFileSystem;
import com.sequenceiq.cloudbreak.api.model.filesystem.S3FileSystem;
import com.sequenceiq.cloudbreak.api.model.filesystem.WasbFileSystem;
import com.sequenceiq.cloudbreak.api.model.v2.StorageLocationRequest;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.domain.StorageLocations;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;

@Component
public class FileSystemRequestToFileSystemConverter extends AbstractConversionServiceAwareConverter<FileSystemRequest, FileSystem> {

    @Inject
    private MissingResourceNameGenerator nameGenerator;

    @Override
    public FileSystem convert(FileSystemRequest source) {
        FileSystem fs = new FileSystem();
        fs.setName(nameGenerator.generateName(FILESYSTEM));
        BaseFileSystem baseFileSystem = getBaseFileSystem(source, fs);
        try {
            fs.setConfigurations(new Json(baseFileSystem));
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Storage configuration could not be parsed: " + source, e);
        }
        StorageLocations storageLocations = new StorageLocations();
        if (source.getLocations() != null && !source.getLocations().isEmpty()) {
            for (StorageLocationRequest storageLocationRequest : source.getLocations()) {
                storageLocations.getLocations().add(getConversionService().convert(storageLocationRequest, StorageLocation.class));
            }
        }
        try {
            fs.setLocations(new Json(storageLocations));
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Storage locations could not be parsed: " + source, e);
        }
        return fs;
    }

    private BaseFileSystem getBaseFileSystem(FileSystemRequest source, FileSystem fs) {
        BaseFileSystem baseFileSystem = null;
        if (source.getAdls() != null) {
            baseFileSystem = getConversionService().convert(source.getAdls(), AdlsFileSystem.class);
            fs.setType(source.getAdls().getType());
        } else if (source.getGcs() != null) {
            baseFileSystem = getConversionService().convert(source.getGcs(), GcsFileSystem.class);
            fs.setType(source.getGcs().getType());
        } else if (source.getS3() != null) {
            baseFileSystem = getConversionService().convert(source.getS3(), S3FileSystem.class);
            fs.setType(source.getS3().getType());
        } else if (source.getWasb() != null) {
            baseFileSystem = getConversionService().convert(source.getWasb(), WasbFileSystem.class);
            fs.setType(source.getWasb().getType());
        } else if (source.getAbfs() != null) {
            baseFileSystem = getConversionService().convert(source.getAbfs(), AbfsFileSystem.class);
            fs.setType(source.getAbfs().getType());
        }
        return baseFileSystem;
    }
}
