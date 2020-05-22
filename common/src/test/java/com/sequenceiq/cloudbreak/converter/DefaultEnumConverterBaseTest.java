package com.sequenceiq.cloudbreak.converter;

import org.junit.jupiter.api.Test;

import javax.persistence.AttributeConverter;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public abstract class DefaultEnumConverterBaseTest<E extends Enum<E>> {

    private static final String NON_BACKWARD_COMPATIBLE_NAME = "non backward compatible";

    public abstract E getDefaultValue();

    public abstract AttributeConverter<E, String> getVictim();

    @Test
    public void shouldConvertToEnumConstantByEnumName() {
        for (Object enumConstant : EnumSet.allOf(getDefaultValue().getClass())) {
            assertEquals((E) enumConstant, getVictim().convertToEntityAttribute(((E) enumConstant).name()));
        }
    }

    @Test
    public void shouldConvertUnexpectedValueToDefault() {
        assertEquals(getDefaultValue(), getVictim().convertToEntityAttribute(NON_BACKWARD_COMPATIBLE_NAME));
    }

    @Test
    public void shouldConvertNullToNull() {
        assertNull(getVictim().convertToDatabaseColumn(null));
        assertNull(getVictim().convertToEntityAttribute(null));
    }

    @Test
    public void shouldConvertEnumConstantToEnumName() {
        for (Object enumConstant : EnumSet.allOf(getDefaultValue().getClass())) {
            assertEquals(((E) enumConstant).name(), getVictim().convertToDatabaseColumn((E) enumConstant));
        }
    }
}
