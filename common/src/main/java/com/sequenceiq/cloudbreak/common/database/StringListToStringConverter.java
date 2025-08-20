package com.sequenceiq.cloudbreak.common.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.AttributeConverter;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class StringListToStringConverter implements AttributeConverter<List<String>, String> {
    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        return CollectionUtils.isEmpty(attribute) ? null : String.join(",", attribute);
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        return StringUtils.isEmpty(dbData) ? new ArrayList<>() : Arrays.asList(dbData.split(","));
    }
}