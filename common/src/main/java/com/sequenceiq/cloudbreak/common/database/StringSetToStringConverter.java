package com.sequenceiq.cloudbreak.common.database;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.AttributeConverter;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class StringSetToStringConverter implements AttributeConverter<Set<String>, String> {
    @Override
    public String convertToDatabaseColumn(Set<String> attribute) {
        return CollectionUtils.isEmpty(attribute) ? null : String.join(",", attribute);
    }

    @Override
    public Set<String> convertToEntityAttribute(String dbData) {
        return StringUtils.isEmpty(dbData) ? new HashSet<>() : new HashSet<>(Arrays.asList(dbData.split(",")));
    }
}
