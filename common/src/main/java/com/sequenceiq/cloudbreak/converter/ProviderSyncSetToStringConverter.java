package com.sequenceiq.cloudbreak.converter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.AttributeConverter;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.common.model.ProviderSyncState;

public class ProviderSyncSetToStringConverter implements AttributeConverter<Set<ProviderSyncState>, String> {

    @Override
    public String convertToDatabaseColumn(Set<ProviderSyncState> providerSyncStates) {
        return CollectionUtils.isEmpty(providerSyncStates) ? null
                : providerSyncStates.stream()
                .map(ProviderSyncState::name)
                .collect(Collectors.joining(","));
    }

    @Override
    public Set<ProviderSyncState> convertToEntityAttribute(String s) {
        ProviderSyncStateConverter providerSyncStateConverter = new ProviderSyncStateConverter();
        return StringUtils.isBlank(s) ? new HashSet<>() :
                Arrays.stream(s.split(","))
                        .map(providerSyncStateConverter::convertToEntityAttribute)
                        .collect(Collectors.toSet());
    }
}