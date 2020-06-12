package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.common.model.FileSystemType;

import javax.persistence.AttributeConverter;

public class FileSystemTypeConverterTest extends DefaultEnumConverterBaseTest<FileSystemType> {

    @Override
    public FileSystemType getDefaultValue() {
        return FileSystemType.S3;
    }

    @Override
    public AttributeConverter<FileSystemType, String> getVictim() {
        return new FileSystemTypeConverter();
    }
}