package com.sequenceiq.cloudbreak.json;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ValidationResultTest {

    private static final Map<String, String> TEST_ERRORS = ImmutableMap.of("field1", "message1", "field2", "message2");

    private ValidationResult result;

    @Before
    public void setup() {
        result = new ValidationResult();
        result.setValidationErrors(TEST_ERRORS);
    }

    @Test
    public void testGetErrors() {
        Assert.assertEquals(TEST_ERRORS, result.getValidationErrors());
    }

    @Test
    public void testSetSingleError() {
        result.setValidationError("field1", "newMessage1");
        Map<String, String> expectedMap = ImmutableMap.of("field1", "newMessage1", "field2", "message2");
        Assert.assertEquals(expectedMap, result.getValidationErrors());
    }

    @Test
    public void testAddSingleError() {
        result.addValidationError("field1", "newMessage1");
        Map<String, String> expectedMap = ImmutableMap.of("field1", "message1; newMessage1", "field2", "message2");
        Assert.assertEquals(expectedMap, result.getValidationErrors());
    }

}
