package com.sequenceiq.cloudbreak.converter.v4.filesystems;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.responses.FileSystemParameterV4Response;
import com.sequenceiq.common.api.cloudstorage.query.ConfigQueryEntry;
import com.sequenceiq.common.model.CloudStorageCdpService;

class ConfigQueryEntryToFileSystemParameterV4ResponseConverterTest {

    private static final String TEST_RELATED_SERVICE = "testForExampleAmbari";

    private static final String TEST_RELATED_SERVICE2 = "testForExampleAmbari2";

    private static final String TEST_DESCRIPTION = "some description value";

    private static final String TEST_PATH_VALUE = "defaultPathValue";

    private static final String TEST_PROPERTY_FILE = "some.file";

    private static final String TEST_PROPERTY_NAME = "testvalue";

    private static final String TEST_DISPLAY_NAME = "name";

    private static final String TEST_STORAGE = "storage";

    private static final String TEST_PROTOCOL = "http";

    private final ConfigQueryEntryToFileSystemParameterV4ResponseConverter underTest = new ConfigQueryEntryToFileSystemParameterV4ResponseConverter();

    @Test
    void testConvertCheckAllPropertyPassedProperly() {
        FileSystemParameterV4Response expected = new FileSystemParameterV4Response();
        expected.setType(CloudStorageCdpService.RANGER_AUDIT.name());
        expected.setDefaultPath(TEST_PATH_VALUE);
        expected.setProtocol(TEST_PROTOCOL);
        expected.setDescription(TEST_DESCRIPTION);
        expected.setPropertyDisplayName(TEST_DISPLAY_NAME);
        expected.setPropertyFile(TEST_PROPERTY_FILE);
        expected.setPropertyName(TEST_PROPERTY_NAME);
        expected.setRelatedMissingServices(new HashSet<>(Arrays.asList(TEST_RELATED_SERVICE, TEST_RELATED_SERVICE2)));
        expected.setRelatedServices(new HashSet<>(Arrays.asList(TEST_RELATED_SERVICE, TEST_RELATED_SERVICE2)));

        FileSystemParameterV4Response result = underTest.convert(createConfigQueryEntry());

        assertEquals(expected, result);
    }

    private ConfigQueryEntry createConfigQueryEntry() {
        ConfigQueryEntry entry = new ConfigQueryEntry();
        entry.setType(CloudStorageCdpService.RANGER_AUDIT);
        entry.setDefaultPath(TEST_PATH_VALUE);
        entry.setProtocol(TEST_PROTOCOL);
        entry.setDescription(TEST_DESCRIPTION);
        entry.setPropertyDisplayName(TEST_DISPLAY_NAME);
        entry.setPropertyFile(TEST_PROPERTY_FILE);
        entry.setPropertyName(TEST_PROPERTY_NAME);
        entry.setRelatedMissingServices(new HashSet<>(Arrays.asList(TEST_RELATED_SERVICE, TEST_RELATED_SERVICE2)));
        entry.setRelatedServices(new HashSet<>(Arrays.asList(TEST_RELATED_SERVICE, TEST_RELATED_SERVICE2)));
        entry.setSupportedStorages(Collections.singleton(TEST_STORAGE));
        return entry;
    }

}