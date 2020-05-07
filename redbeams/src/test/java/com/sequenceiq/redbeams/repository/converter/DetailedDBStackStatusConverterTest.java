package com.sequenceiq.redbeams.repository.converter;

import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DetailedDBStackStatusConverterTest {

    private static final String NON_BACKWARD_COMPATIBLE_NAME = "non bakward compatible";

    private DetailedDBStackStatusConverter victim;

    @BeforeEach
    public void initTest() {
        victim = new DetailedDBStackStatusConverter();
    }

    @ParameterizedTest
    @EnumSource(DetailedDBStackStatus.class)
    public void shouldConvertToEnumByEnumName(DetailedDBStackStatus detailedDBStackStatus) {
        String name = detailedDBStackStatus.name();

        assertEquals(detailedDBStackStatus, victim.convertToEntityAttribute(name));
    }

    @Test
    public void shouldConvertUnexpectedValueToAvailable() {
        assertEquals(DetailedDBStackStatus.AVAILABLE, victim.convertToEntityAttribute(NON_BACKWARD_COMPATIBLE_NAME));
    }

    @Test
    public void shouldConvertNullToNull() {
        assertNull(victim.convertToDatabaseColumn(null));
        assertNull(victim.convertToEntityAttribute(null));
    }

    @ParameterizedTest
    @EnumSource(DetailedDBStackStatus.class)
    public void shouldConvertEnumToEnumName(DetailedDBStackStatus detailedDBStackStatus) {
        assertEquals(detailedDBStackStatus.name(), victim.convertToDatabaseColumn(detailedDBStackStatus));
    }
}