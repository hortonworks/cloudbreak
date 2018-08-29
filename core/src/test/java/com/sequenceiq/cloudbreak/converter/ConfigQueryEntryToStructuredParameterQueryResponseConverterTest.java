package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.StructuredParameterQueryResponse;
import com.sequenceiq.cloudbreak.template.filesystem.query.ConfigQueryEntry;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class ConfigQueryEntryToStructuredParameterQueryResponseConverterTest {

    private static final String TEST_RELATED_SERVICE = "testForExampleAmbar";

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
        expected.setRelatedService(TEST_RELATED_SERVICE);

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
        entry.setRelatedService(TEST_RELATED_SERVICE);
        entry.setSupportedStorages(Collections.singleton(TEST_STORAGE));
        return entry;
    }

}