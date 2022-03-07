package com.sequenceiq.cloudbreak.structuredevent.converter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.domain.CustomConfigurationProperty;
import com.sequenceiq.cloudbreak.domain.CustomConfigurations;
import com.sequenceiq.cloudbreak.structuredevent.event.CustomConfigurationsDetails;

@ExtendWith(MockitoExtension.class)
class CustomConfigurationsToCustomConfigurationsDetailsConverterTest {

    @InjectMocks
    private CustomConfigurationsToCustomConfigurationsDetailsConverter underTest;

    @Test
    void testConvertEmptyNoNPE() {
        CustomConfigurations customConfigurations = new CustomConfigurations();

        CustomConfigurationsDetails customConfigurationsDetails = underTest.convert(customConfigurations);

        assertThat(customConfigurationsDetails).isNotNull();
    }

    @Test
    void testConvert() {
        CustomConfigurations customConfigurations = new CustomConfigurations();
        customConfigurations.setId(1L);
        customConfigurations.setName("test-name");
        customConfigurations.setConfigurations(Sets.newHashSet(
                new CustomConfigurationProperty("property1", "value1", null, "service1"),
                new CustomConfigurationProperty("property2", "value2", "role2", "service2"),
                new CustomConfigurationProperty("property3", "value3", null, "service3"),
                new CustomConfigurationProperty("property4", "value4", "role4", "service4")));
        customConfigurations.setRuntimeVersion("test-runtime-version");

        CustomConfigurationsDetails customConfigurationsDetails = underTest.convert(customConfigurations);

        assertThat(customConfigurationsDetails).isNotNull();
        assertThat(customConfigurationsDetails.getCustomConfigurationsName()).isEqualTo("test-name");
        assertThat(customConfigurationsDetails.getId()).isEqualTo(1L);
        assertThat(customConfigurationsDetails.getRuntimeVersion()).isEqualTo("test-runtime-version");
        assertThat(customConfigurationsDetails.getRoles()).hasSameElementsAs(Lists.newArrayList("role2", "role4"));
        assertThat(customConfigurationsDetails.getServices()).hasSameElementsAs(Lists.newArrayList("service1", "service2", "service3", "service4"));
    }

}