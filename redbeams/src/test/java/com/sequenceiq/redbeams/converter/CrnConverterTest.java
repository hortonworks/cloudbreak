package com.sequenceiq.redbeams.converter;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnParseException;
import com.sequenceiq.redbeams.TestData;

public class CrnConverterTest {

    private static final String RESOURCE_NAME = "resourceName";

    private CrnConverter underTest;

    private Crn validCrn;

    @Before
    public void setup() {
        underTest = new CrnConverter();

        validCrn = TestData.getTestCrn("database", "name");
    }

    @Test
    public void testConvertToDbField() {
        String dbField = underTest.convertToDatabaseColumn(validCrn);

        assertEquals(validCrn.toString(), dbField);
    }

    @Test
    public void testConvertToEntityAttribute() {
        Crn dbCrn = underTest.convertToEntityAttribute(validCrn.toString());

        assertEquals(validCrn, dbCrn);
    }

    @Test(expected = CrnParseException.class)
    public void testConvertToEntityAttributeInvalidCrnString() {
        underTest.convertToEntityAttribute("nope");
    }
}
