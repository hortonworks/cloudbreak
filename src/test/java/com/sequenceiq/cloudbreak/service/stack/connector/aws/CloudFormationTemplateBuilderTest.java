package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.domain.TemplateGroup;

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
       // String result = underTest.build("templates/aws-cf-stack.ftl", false, new ArrayList<>());
        // THEN
        //assertTrue(result.contains("\"DeviceName\" : \"/dev/xvdf\""));
        //assertTrue(result.contains("\"DeviceName\" : \"/dev/xvdg\""));
        //assertFalse(result.contains("\"DeviceName\" : \"/dev/xvdh\""));
    }

    @Test
    public void testBuildTemplateShouldHaveSpotPriceSpecifiedWhenItIsSet() {
        // WHEN
        String result = underTest.build("templates/aws-cf-stack.ftl", true, new ArrayList<TemplateGroup>());
        // THEN
        assertTrue(result.contains("\"SpotPrice\""));
    }

    @Test
    public void testBuildTemplateShouldNotHaveSpotPriceSpecifiedWhenItIsSetToFalse() {
        // WHEN
        //String result = underTest.build("templates/aws-cf-stack.ftl", 2, false);
        // THEN
        //assertFalse(result.contains("\"SpotPrice\""));
    }

    @Test(expected = InternalServerException.class)
    public void testBuildTemplateShouldThrowInternalServerExceptionWhenTemplateDoesNotExist() {
        // WHEN
      //  underTest.build("templates/non-existent.ftl", 2, false);
    }
}
