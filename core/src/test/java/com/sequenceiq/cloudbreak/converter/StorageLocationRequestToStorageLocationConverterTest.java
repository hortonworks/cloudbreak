package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.v2.StorageLocationRequest;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem.StorageLocationV4RequestToStorageLocationConverter;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StorageLocationRequestToStorageLocationConverterTest {

    private static final String PROPERTY_FILE = "/some/file";

    private static final String PROPERTY_NAME = "path";

    private static final String VALUE = "propertyValue";

    private StorageLocationV4RequestToStorageLocationConverter underTest;

    @Before
    public void setUp() {
        underTest = new StorageLocationV4RequestToStorageLocationConverter();
    }

    @Test
    public void testConvertWhenPassingStorageLocationRequestThenEveryNecessaryParametersShouldBePassed() {
        StorageLocation expected = new StorageLocation();
        expected.setConfigFile(PROPERTY_FILE);
        expected.setProperty(PROPERTY_NAME);
        expected.setValue(VALUE);

        StorageLocation result = underTest.convert(createSource());

        assertEquals(expected, result);
    }

    private StorageLocationRequest createSource() {
        StorageLocationRequest request = new StorageLocationRequest();
        request.setPropertyFile(PROPERTY_FILE);
        request.setPropertyName(PROPERTY_NAME);
        request.setValue(VALUE);
        return request;
    }

}