package com.sequenceiq.cloudbreak.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.storage.location.StorageLocationV4Response;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem.StorageLocationToStorageLocationV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.StorageLocation;

class StorageLocationToStorageLocationResponseConverterTest {

    private static final String PROPERTY_FILE = "/some/file";

    private static final String PROPERTY_NAME = "path";

    private static final String VALUE = "propertyValue";

    private StorageLocationToStorageLocationV4ResponseConverter underTest = new StorageLocationToStorageLocationV4ResponseConverter();

    @Test
    void testConvertWhenPassingStorageLocationThenAllNecessaryParametersShouldBePassed() {
        StorageLocationV4Response expected = new StorageLocationV4Response();
        expected.setValue(VALUE);
        expected.setPropertyName(PROPERTY_NAME);
        expected.setPropertyFile(PROPERTY_FILE);

        StorageLocationV4Response result = underTest.convert(createSource());

        assertEquals(expected, result);
    }

    private StorageLocation createSource() {
        StorageLocation response = new StorageLocation();
        response.setConfigFile(PROPERTY_FILE);
        response.setProperty(PROPERTY_NAME);
        response.setValue(VALUE);
        return response;
    }

}