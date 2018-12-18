package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.StructuredParameterQueryResponse;
import com.sequenceiq.cloudbreak.template.filesystem.query.ConfigQueryEntry;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;

public class ConfigQueryEntryToStructuredParameterQueryResponseConverterTest {

    private static final String TEST_RELATED_SERVICE = "testForExampleAmbari";

    private static final String TEST_RELATED_SERVICE2 = "testForExampleAmbari2";

    private static final String TEST_DESCRIPTION = "some description value";

    private static final String TEST_PATH_VALUE = "defaultPathValue";

    private static final String TEST_PROPERTY_FILE = "some.file";

    private static final String TEST_PROPERTY_NAME = "testvalue";

    private static final String TEST_DISPLAY_NAME = "name";

    private static final String TEST_STORAGE = "storage";

    private static final String TEST_PROTOCOL = "http";

    private ConfigQueryEntryToStructuredParameterQueryResponseConverter underTest;

    @Before
    public void setUp() {
        underTest = new ConfigQueryEntryToStructuredParameterQueryResponseConverter();
    }

    @Test
    public void testConvertCheckAllPropertyPassedProperly() {
        StructuredParameterQueryResponse expected = new StructuredParameterQueryResponse();
        expected.setDefaultPath(TEST_PATH_VALUE);
        expected.setProtocol(TEST_PROTOCOL);
        expected.setDescription(TEST_DESCRIPTION);
        expected.setPropertyDisplayName(TEST_DISPLAY_NAME);
        expected.setPropertyFile(TEST_PROPERTY_FILE);
        expected.setPropertyName(TEST_PROPERTY_NAME);
        expected.setRelatedServices(new HashSet<>(Arrays.asList(TEST_RELATED_SERVICE, TEST_RELATED_SERVICE2)));

        StructuredParameterQueryResponse result = underTest.convert(createConfigQueryEntry());

        assertEquals(expected, result);
    }

    private ConfigQueryEntry createConfigQueryEntry() {
        ConfigQueryEntry entry = new ConfigQueryEntry();
        entry.setDefaultPath(TEST_PATH_VALUE);
        entry.setProtocol(TEST_PROTOCOL);
        entry.setDescription(TEST_DESCRIPTION);
        entry.setPropertyDisplayName(TEST_DISPLAY_NAME);
        entry.setPropertyFile(TEST_PROPERTY_FILE);
        entry.setPropertyName(TEST_PROPERTY_NAME);
        entry.setRelatedServices(new HashSet<>(Arrays.asList(TEST_RELATED_SERVICE, TEST_RELATED_SERVICE2)));
        entry.setSupportedStorages(Collections.singleton(TEST_STORAGE));
        return entry;
    }

}