package com.sequenceiq.cloudbreak.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.requests.CustomConfigurationsV4Request;
import com.sequenceiq.cloudbreak.api.model.CustomConfigurationPropertyParameters;
import com.sequenceiq.cloudbreak.domain.CustomConfigurationProperty;
import com.sequenceiq.cloudbreak.domain.CustomConfigurations;

class CustomConfigurationsV4RequestToCustomConfigurationsConverterTest {

    @Mock
    private CustomConfigurationPropertyConverter customConfigurationPropertyConverter;

    @InjectMocks
    private CustomConfigurationsV4RequestToCustomConfigurationsConverter underTest;

    private final CustomConfigurationsV4Request request = new CustomConfigurationsV4Request();

    private final  CustomConfigurationPropertyParameters property = new CustomConfigurationPropertyParameters();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testConvert() {
        property.setName("property1");
        property.setValue("value1");
        property.setRoleType("role1");
        property.setServiceType("service1");
        request.setName("test");
        request.setConfigurations(Set.of(property));
        request.setRuntimeVersion("7.2.8");
        when(customConfigurationPropertyConverter.convertFromRequestJson(any(CustomConfigurationPropertyParameters.class)))
                .thenReturn(new CustomConfigurationProperty("property1", "value1", "role1", "service1"));
        when(customConfigurationPropertyConverter.convertToResponseJson(any(CustomConfigurationProperty.class))).thenReturn(property);

        CustomConfigurations result = underTest.convert(request);

        assertEquals(request.getName(), result.getName());
        assertEquals(request.getConfigurations(), result.getConfigurations()
                .stream()
                .map(c -> customConfigurationPropertyConverter.convertToResponseJson(c))
                .collect(Collectors.toSet()));
        assertEquals(request.getRuntimeVersion(), result.getRuntimeVersion());
    }
}