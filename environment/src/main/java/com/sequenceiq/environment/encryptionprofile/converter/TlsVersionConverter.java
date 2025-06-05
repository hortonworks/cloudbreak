package com.sequenceiq.environment.encryptionprofile.converter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.sequenceiq.common.api.encryptionprofile.TlsVersion;

@Converter
public class TlsVersionConverter implements AttributeConverter<Set<TlsVersion>, String> {

    @Override
    public String convertToDatabaseColumn(Set<TlsVersion> attribute) {
        return attribute != null ? attribute.stream()
                .map(TlsVersion::getVersion)
                .collect(Collectors.joining(",")) : "";
    }

    @Override
    public Set<TlsVersion> convertToEntityAttribute(String dbData) {
        return dbData != null && !dbData.isEmpty() ?
                Arrays.stream(dbData.split(","))
                        .map(TlsVersion::fromString)
                        .collect(Collectors.toSet()) : EnumSet.noneOf(TlsVersion.class);
    }
}

