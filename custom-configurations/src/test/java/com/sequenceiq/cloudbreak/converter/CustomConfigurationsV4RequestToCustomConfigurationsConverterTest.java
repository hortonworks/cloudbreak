package com.sequenceiq.cloudbreak.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.requests.CustomConfigurationsV4Request;
import com.sequenceiq.cloudbreak.api.model.CustomConfigurationPropertyParameters;
import com.sequenceiq.cloudbreak.domain.CustomConfigurations;

class CustomConfigurationsV4RequestToCustomConfigurationsConverterTest {

    private CustomConfigurationsV4RequestToCustomConfigurationsConverter underTest;

    private CustomConfigurationsV4Request request = new CustomConfigurationsV4Request();

    @BeforeEach
    void setUp() {
        underTest = new CustomConfigurationsV4RequestToCustomConfigurationsConverter();
    }

    @Test
    void testConvert() {
        CustomConfigurationPropertyParameters property = new CustomConfigurationPropertyParameters();
        property.setName("property1");
        property.setValue("value1");
        property.setRoleType("role1");
        property.setServiceType("service1");
        request.setName("test");
        request.setConfigurations(Set.of(property));
        request.setRuntimeVersion("7.2.8");
        CustomConfigurations result = underTest.convert(request);
        assertEquals(request.getName(), result.getName());
        assertEquals(request.getConfigurations(), result.getConfigurations()
                .stream()
                .map(CustomConfigurationPropertyConverter::convertTo)
                .collect(Collectors.toSet()));
        assertEquals(request.getRuntimeVersion(), result.getRuntimeVersion());
    }
}