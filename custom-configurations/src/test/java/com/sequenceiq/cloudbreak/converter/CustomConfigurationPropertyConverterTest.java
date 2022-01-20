package com.sequenceiq.cloudbreak.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.model.CustomConfigurationPropertyParameters;
import com.sequenceiq.cloudbreak.domain.CustomConfigurationProperty;

class CustomConfigurationPropertyConverterTest {

    private static final String TEST_PROPERTY_NAME = "propertyName";

    private static final String TEST_PROPERTY_VALUE = "propertyValue";

    private static final String TEST_ROLE = "roleType";

    private static final String TEST_SERVICE = "serviceType";

    private CustomConfigurationPropertyConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new CustomConfigurationPropertyConverter();
    }

    @Test
    void testConvertFromCustomConfigurationPropertyParameters() {
        CustomConfigurationPropertyParameters property = new CustomConfigurationPropertyParameters();
        property.setName(TEST_PROPERTY_NAME);
        property.setValue(TEST_PROPERTY_VALUE);
        property.setRoleType(TEST_ROLE);
        property.setServiceType(TEST_SERVICE);

        CustomConfigurationProperty result = underTest.convertFromRequestJson(property);

        assertEquals(TEST_PROPERTY_NAME, result.getName());
        assertEquals(TEST_PROPERTY_VALUE, result.getValue());
        assertEquals(TEST_ROLE, result.getRoleType());
        assertEquals(TEST_SERVICE, result.getServiceType());
    }

    @Test
    void testConvertToCustomConfigurationPropertyParameters() {
        CustomConfigurationProperty property = new CustomConfigurationProperty();
        property.setName(TEST_PROPERTY_NAME);
        property.setSecretValue(TEST_PROPERTY_VALUE);
        property.setRoleType(TEST_ROLE);
        property.setServiceType(TEST_SERVICE);

        CustomConfigurationPropertyParameters result = underTest.convertToResponseJson(property);

        assertEquals(TEST_PROPERTY_NAME, result.getName());
        assertEquals(TEST_PROPERTY_VALUE, result.getValue());
        assertEquals(TEST_ROLE, result.getRoleType());
        assertEquals(TEST_SERVICE, result.getServiceType());
    }
}