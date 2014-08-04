package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.sequenceiq.cloudbreak.controller.InternalServerException;

import freemarker.template.Configuration;

public class CloudFormationTemplateBuilderTest {

    @InjectMocks
    private CloudFormationTemplateBuilder underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new CloudFormationTemplateBuilder();
        MockitoAnnotations.initMocks(this);
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        Configuration freemarkerConfiguration = factoryBean.getObject();
        underTest.setFreemarkerConfiguration(freemarkerConfiguration);
    }

    @Test
    public void testBuildTemplateShouldCreateTwoDeviceNameEntriesWhenTwoVolumesAreSpecified() {
        // WHEN
        String result = underTest.build("templates/aws-cf-stack.ftl", 2);
        // THEN
        assertTrue(result.contains("\"DeviceName\" : \"/dev/sdf\""));
        assertTrue(result.contains("\"DeviceName\" : \"/dev/sdg\""));
        assertFalse(result.contains("\"DeviceName\" : \"/dev/sdh\""));
    }

    @Test(expected = InternalServerException.class)
    public void testBuildTemplateShouldThrowInternalServerExceptionWhenTemplateDoesNotExist() {
        // WHEN
        underTest.build("templates/non-existent.ftl", 2);
    }
}
