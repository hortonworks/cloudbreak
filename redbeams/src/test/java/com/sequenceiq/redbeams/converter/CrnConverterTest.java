package com.sequenceiq.redbeams.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnParseException;
import com.sequenceiq.redbeams.TestData;

class CrnConverterTest {

    private static final String RESOURCE_NAME = "resourceName";

    private CrnConverter underTest;

    private Crn validCrn;

    @BeforeEach
    public void setup() {
        underTest = new CrnConverter();

        validCrn = TestData.getTestCrn("database", "name");
    }

    @Test
    void testConvertToDbField() {
        String dbField = underTest.convertToDatabaseColumn(validCrn);

        assertEquals(validCrn.toString(), dbField);
    }

    @Test
    void testConvertToEntityAttribute() {
        Crn dbCrn = underTest.convertToEntityAttribute(validCrn.toString());

        assertEquals(validCrn, dbCrn);
    }

    @Test
    void testConvertToEntityAttributeInvalidCrnString() {
        assertThrows(CrnParseException.class, () -> underTest.convertToEntityAttribute("nope"));
    }
}
