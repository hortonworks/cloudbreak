package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.common.model.FileSystemType;

public class FileSystemTypeConverter extends DefaultEnumConverter<FileSystemType> {

    @Override
    public FileSystemType getDefault() {
        return FileSystemType.S3;
    }
}
