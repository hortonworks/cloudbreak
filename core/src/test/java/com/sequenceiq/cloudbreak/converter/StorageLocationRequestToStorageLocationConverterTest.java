package com.sequenceiq.cloudbreak.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.location.StorageLocationV4Request;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem.StorageLocationV4RequestToStorageLocationConverter;
import com.sequenceiq.cloudbreak.domain.StorageLocation;

class StorageLocationRequestToStorageLocationConverterTest {

    private static final String PROPERTY_FILE = "/some/file";

    private static final String PROPERTY_NAME = "path";

    private static final String VALUE = "propertyValue";

    private StorageLocationV4RequestToStorageLocationConverter underTest = new StorageLocationV4RequestToStorageLocationConverter();

    @Test
    void testConvertWhenPassingStorageLocationRequestThenEveryNecessaryParametersShouldBePassed() {
        StorageLocation expected = new StorageLocation();
        expected.setConfigFile(PROPERTY_FILE);
        expected.setProperty(PROPERTY_NAME);
        expected.setValue(VALUE);

        StorageLocation result = underTest.convert(createSource());

        assertEquals(expected, result);
    }

    private StorageLocationV4Request createSource() {
        StorageLocationV4Request request = new StorageLocationV4Request();
        request.setPropertyFile(PROPERTY_FILE);
        request.setPropertyName(PROPERTY_NAME);
        request.setValue(VALUE);
        return request;
    }

}