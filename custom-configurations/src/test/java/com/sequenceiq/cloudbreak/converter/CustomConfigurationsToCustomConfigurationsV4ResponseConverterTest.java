package com.sequenceiq.cloudbreak.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.responses.CustomConfigurationsV4Response;
import com.sequenceiq.cloudbreak.api.model.CustomConfigurationPropertyParameters;
import com.sequenceiq.cloudbreak.domain.CustomConfigurationProperty;
import com.sequenceiq.cloudbreak.domain.CustomConfigurations;

class CustomConfigurationsToCustomConfigurationsV4ResponseConverterTest {

    private CustomConfigurationsToCustomConfigurationsV4ResponseConverter underTest;

    private CustomConfigurations customConfigurations = new CustomConfigurations("test",
            "crn:cdp:resource:us-west-1:tenant:customconfigs:c7da2918-dd14-49ed-9b43-33ff55bd6309",
            Set.of(new CustomConfigurationProperty("property1", "value1", "role1", "service1")),
            "7.2.8",
            "accid",
            System.currentTimeMillis()
            );

    @BeforeEach
    void setUp() {
        underTest = new CustomConfigurationsToCustomConfigurationsV4ResponseConverter();
    }

    @Test
    void testConvert() {
        customConfigurations.setAccount("testAccount");
        CustomConfigurationsV4Response response = underTest.convert(customConfigurations);
        assertEquals(customConfigurations.getName(), response.getName());
        assertEquals(customConfigurations.getConfigurations(), response.getConfigurations()
                .stream()
                .map(CustomConfigurationPropertyConverter::convertFrom)
                .collect(Collectors.toSet()));
        assertEquals(customConfigurations.getRuntimeVersion(), response.getRuntimeVersion());
        assertEquals(customConfigurations.getCrn(), response.getCrn());
        assertEquals(customConfigurations.getAccount(), response.getAccount());
        assertEquals(customConfigurations.getCreated(), response.getCreated());
    }

    @Test
    void testConvertThrowsExceptionIfConfigsAreNull() {
        customConfigurations.setConfigurations(null);
        assertThrows(NullPointerException.class, () -> underTest.convert(customConfigurations));
    }

    @Test
    void testConvertResponseContainsCorrectCustomConfigsProperties() {
        Set<CustomConfigurationPropertyParameters> properties = customConfigurations.getConfigurations().stream()
                .map(CustomConfigurationPropertyConverter::convertTo).collect(Collectors.toSet());
        CustomConfigurationsV4Response response = underTest.convert(customConfigurations);
        Set<CustomConfigurationPropertyParameters> result = response.getConfigurations();
        assertEquals(properties, result);
    }
}