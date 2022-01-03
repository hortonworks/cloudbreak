package com.sequenceiq.cloudbreak.converter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;

class TemporaryStorageConverterTest {

    private final TemporaryStorageConverter underTest = new TemporaryStorageConverter();

    @Test
    void temporaryStorageEnumToDbColumn() {
        assertThat(underTest.convertToDatabaseColumn(TemporaryStorage.ATTACHED_VOLUMES)).isEqualTo("ATTACHED_VOLUMES");
    }

    @Test
    void nonNullDbColumnToTemporaryStorageEnum() {
        assertThat(underTest.convertToEntityAttribute("EPHEMERAL_VOLUMES")).isEqualTo(TemporaryStorage.EPHEMERAL_VOLUMES);
    }

    @Test
    void nullDbColumnToTemporaryStorageEnum() {
        assertThat(underTest.convertToEntityAttribute(null)).isEqualTo(TemporaryStorage.ATTACHED_VOLUMES);
    }

}