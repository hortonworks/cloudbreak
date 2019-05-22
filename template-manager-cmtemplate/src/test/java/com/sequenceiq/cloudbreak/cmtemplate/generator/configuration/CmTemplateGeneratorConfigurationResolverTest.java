package com.sequenceiq.cloudbreak.cmtemplate.generator.configuration;

import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.domain.StackVersion;
import com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.domain.dependencies.ServiceConfig;

@RunWith(MockitoJUnitRunner.class)
public class CmTemplateGeneratorConfigurationResolverTest {

    @InjectMocks
    private final CmTemplateGeneratorConfigurationResolver underTest = new CmTemplateGeneratorConfigurationResolver();

    @Before
    public void setup() {
        ReflectionTestUtils.setField(underTest, "cdhConfigurationsPath", "cloudera-manager-template/cdh");
        ReflectionTestUtils.setField(underTest, "serviceDefinitionConfigurationPath", "cloudera-manager-template/service-definitions-minimal.json");
        underTest.prepareConfigs();
    }

    @Test
    public void testThatAllFileIsReadableShouldVerifyThatFileCountMatch() {
        Map<StackVersion, Set<String>> stackVersionSetMap = underTest.cdhConfigurations();
        Set<ServiceConfig> serviceConfigs = underTest.serviceConfigs();

        Assert.assertEquals(2L, stackVersionSetMap.size());
        Assert.assertEquals(13L, serviceConfigs.size());
    }
}