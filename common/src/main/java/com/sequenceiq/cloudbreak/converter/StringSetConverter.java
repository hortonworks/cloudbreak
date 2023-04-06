package com.sequenceiq.cloudbreak.converter;

import java.util.Set;

import javax.persistence.AttributeConverter;

import org.springframework.stereotype.Component;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

@Component
public class StringSetConverter implements AttributeConverter<Set<String>, String> {

    @Override
    public String convertToDatabaseColumn(Set<String> set) {
        return set == null || set.isEmpty() ?
                null : Joiner.on(',').join(set);
    }

    @Override
    public Set<String> convertToEntityAttribute(String joined) {
        return Strings.isNullOrEmpty(joined) ? null : Set.of(joined.split(","));
    }
}