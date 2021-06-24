package com.sequenceiq.cloudbreak.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.requests.CustomConfigsV4Request;
import com.sequenceiq.cloudbreak.api.model.CustomConfigPropertyParameters;
import com.sequenceiq.cloudbreak.domain.CustomConfigs;

class CustomConfigsV4RequestToCustomConfigsConverterTest {

    private CustomConfigsV4RequestToCustomConfigsConverter underTest;

    private CustomConfigsV4Request request = new CustomConfigsV4Request();

    @BeforeEach
    void setUp() {
        underTest = new CustomConfigsV4RequestToCustomConfigsConverter();
    }

    @Test
    void testConvert() {
        CustomConfigPropertyParameters property = new CustomConfigPropertyParameters();
        property.setConfigName("property1");
        property.setConfigValue("value1");
        property.setRoleType("role1");
        property.setServiceType("service1");
        request.setName("test");
        request.setConfigs(Set.of(property));
        request.setPlatformVersion("7.2.8");
        CustomConfigs result = underTest.convert(request);
        assertEquals(request.getName(), result.getName());
        assertEquals(request.getConfigs(), result.getConfigs().stream().map(CustomConfigPropertyConverter::convertTo).collect(Collectors.toSet()));
        assertEquals(request.getPlatformVersion(), result.getPlatformVersion());
    }
}