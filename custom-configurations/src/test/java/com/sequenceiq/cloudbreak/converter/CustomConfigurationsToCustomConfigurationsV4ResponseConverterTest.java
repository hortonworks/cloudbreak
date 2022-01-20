package com.sequenceiq.cloudbreak.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.responses.CustomConfigurationsV4Response;
import com.sequenceiq.cloudbreak.api.model.CustomConfigurationPropertyParameters;
import com.sequenceiq.cloudbreak.domain.CustomConfigurationProperty;
import com.sequenceiq.cloudbreak.domain.CustomConfigurations;

class CustomConfigurationsToCustomConfigurationsV4ResponseConverterTest {

    @Mock
    private CustomConfigurationPropertyConverter customConfigurationPropertyConverter;

    @InjectMocks
    private CustomConfigurationsToCustomConfigurationsV4ResponseConverter underTest;

    private final CustomConfigurations customConfigurations = new CustomConfigurations("test",
            "crn:cdp:resource:us-west-1:tenant:customconfigs:c7da2918-dd14-49ed-9b43-33ff55bd6309",
            Set.of(new CustomConfigurationProperty("property1", "value1", "role1", "service1"),
                    new CustomConfigurationProperty("property2", "value2", null, "service2")),
            "7.2.8",
            "accid",
            System.currentTimeMillis()
            );

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testConvert() {
        customConfigurations.setAccount("testAccount");
        when(customConfigurationPropertyConverter.convertToResponseJson(any(CustomConfigurationProperty.class))).thenCallRealMethod();

        CustomConfigurationsV4Response response = underTest.convert(customConfigurations);

        assertEquals(customConfigurations.getName(), response.getName());
        assertTrue(CollectionUtils.isEqualCollection(Sets.newHashSet("property1", "property2"),
                collectNames(response.getConfigurations())));
        assertTrue(CollectionUtils.isEqualCollection(Sets.newHashSet("value1", "value2"),
                collectValues(response.getConfigurations())));
        assertTrue(CollectionUtils.isEqualCollection(Sets.newHashSet("role1", null),
                collectRoles(response.getConfigurations())));
        assertTrue(CollectionUtils.isEqualCollection(Sets.newHashSet("service2", "service1"),
                collectServices(response.getConfigurations())));
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
                .map(c -> customConfigurationPropertyConverter.convertToResponseJson(c)).collect(Collectors.toSet());
        CustomConfigurationsV4Response response = underTest.convert(customConfigurations);
        Set<CustomConfigurationPropertyParameters> result = response.getConfigurations();
        assertEquals(properties, result);
    }

    private Set<String> collectNames(Collection<CustomConfigurationPropertyParameters> properties) {
        return properties.stream().map(CustomConfigurationPropertyParameters::getName).collect(Collectors.toSet());
    }

    private Set<String> collectValues(Collection<CustomConfigurationPropertyParameters> properties) {
        return properties.stream().map(CustomConfigurationPropertyParameters::getValue).collect(Collectors.toSet());
    }

    private Set<String> collectServices(Collection<CustomConfigurationPropertyParameters> properties) {
        return properties.stream().map(CustomConfigurationPropertyParameters::getServiceType).collect(Collectors.toSet());
    }

    private Set<String> collectRoles(Collection<CustomConfigurationPropertyParameters> properties) {
        return properties.stream().map(CustomConfigurationPropertyParameters::getRoleType).collect(Collectors.toSet());
    }
}