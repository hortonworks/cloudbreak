package com.sequenceiq.cloudbreak.converter;

import java.util.Set;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.google.common.base.Joiner;

@Converter
public class StringSetConverter implements AttributeConverter<Set<String>, String> {

    @Override
    public String convertToDatabaseColumn(Set<String> set) {
        return set != null && !set.isEmpty() ? Joiner.on(',').join(set) : null;
    }

    @Override
    public Set<String> convertToEntityAttribute(String joined) {
        return joined != null ? Set.of(joined.split(",")) : null;
    }
}