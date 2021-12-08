package com.sequenceiq.flow.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.flow.domain.ClassValue;

class ClassValueConverterTest {

    private final ClassValueConverter underTest = new ClassValueConverter();

    @Test
    public void testFromDatabaseWithKnownClass() {
        ClassValue classValue = underTest.convertToEntityAttribute("java.lang.String");

        assertTrue(classValue.isOnClassPath());
        assertEquals("java.lang.String", classValue.getName());
        assertEquals(String.class, classValue.getClassValue());
    }

    @Test
    public void testFromDatabaseWithUnknownClass() {
        ClassValue classValue = underTest.convertToEntityAttribute("nope.Nope");

        assertFalse(classValue.isOnClassPath());
        assertEquals("nope.Nope", classValue.getName());
        IllegalStateException exception = assertThrows(IllegalStateException.class, classValue::getClassValue);
        assertEquals("nope.Nope is not a known class.", exception.getMessage());
    }

    @Test
    public void testToDatabaseWithKnownClass() throws ClassNotFoundException {
        String result = underTest.convertToDatabaseColumn(ClassValue.of("java.lang.String"));
        assertEquals("java.lang.String", result);
    }

    @Test
    public void testToDatabaseWithUnknownClass() {
        String result = underTest.convertToDatabaseColumn(ClassValue.ofUnknown("nope.Nope"));
        assertEquals("nope.Nope", result);
    }

    @Test
    public void testGetSimpleName() {
        assertEquals("Nope", ClassValue.ofUnknown("com.nope.Nope").getSimpleName());

        assertEquals("String", ClassValue.of(String.class).getSimpleName());
    }
}