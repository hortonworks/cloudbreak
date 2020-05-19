package com.sequenceiq.datalake.converter;

import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DatalakeStatusEnumConverterTest {

    private static final String NON_BACKWARD_COMPATIBLE_NAME = "non bakward compatible";

    private DatalakeStatusEnumConverter victim;

    @BeforeEach
    public void initTest() {
        victim = new DatalakeStatusEnumConverter();
    }

    @ParameterizedTest
    @EnumSource(DatalakeStatusEnum.class)
    public void shouldConvertToEnumByEnumName(DatalakeStatusEnum datalakeStatusEnum) {
        String name = datalakeStatusEnum.name();

        assertEquals(datalakeStatusEnum, victim.convertToEntityAttribute(name));
    }

    @Test
    public void shouldConvertUnexpectedValueToRunning() {
        assertEquals(DatalakeStatusEnum.RUNNING, victim.convertToEntityAttribute(NON_BACKWARD_COMPATIBLE_NAME));
    }

    @ParameterizedTest
    @EnumSource(DatalakeStatusEnum.class)
    public void shouldConvertEnumToEnumName(DatalakeStatusEnum datalakeStatusEnum) {
        assertEquals(datalakeStatusEnum.name(), victim.convertToDatabaseColumn(datalakeStatusEnum));
    }
}