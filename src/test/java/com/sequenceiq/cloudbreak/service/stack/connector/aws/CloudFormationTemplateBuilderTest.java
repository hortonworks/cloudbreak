package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.VolumeType;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
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
        TemplateGroup templateGroup1 = new TemplateGroup();
        AwsTemplate awsTemplate = new AwsTemplate();
        awsTemplate.setInstanceType(InstanceType.C1Medium);
        awsTemplate.setSpotPrice(0.2);
        awsTemplate.setSshLocation("0.0.0.0/0");
        awsTemplate.setName("awstemp1");
        awsTemplate.setVolumeCount(2);
        awsTemplate.setVolumeSize(100);
        awsTemplate.setVolumeType(VolumeType.Gp2);
        templateGroup1.setNodeCount(1);
        templateGroup1.setTemplate(awsTemplate);
        templateGroup1.setGroupName("master");
        // WHEN
        String result = underTest.build("templates/aws-cf-stack.ftl", false, Arrays.asList(templateGroup1));
        // THEN
        assertTrue(result.contains("\"DeviceName\" : \"/dev/xvdf\""));
        assertTrue(result.contains("\"DeviceName\" : \"/dev/xvdg\""));
        assertFalse(result.contains("\"DeviceName\" : \"/dev/xvdh\""));
    }

    @Test
    public void testBuildTemplateShouldHaveSpotPriceSpecifiedWhenItIsSet() {
        TemplateGroup templateGroup1 = new TemplateGroup();
        AwsTemplate awsTemplate = new AwsTemplate();
        awsTemplate.setInstanceType(InstanceType.C1Medium);
        awsTemplate.setSpotPrice(0.2);
        awsTemplate.setSshLocation("0.0.0.0/0");
        awsTemplate.setName("awstemp1");
        awsTemplate.setVolumeCount(1);
        awsTemplate.setVolumeSize(100);
        awsTemplate.setVolumeType(VolumeType.Gp2);
        templateGroup1.setNodeCount(1);
        templateGroup1.setTemplate(awsTemplate);
        templateGroup1.setGroupName("master");
        // WHEN
        String result = underTest.build("templates/aws-cf-stack.ftl", true, Arrays.asList(templateGroup1));
        // THEN
        assertTrue(result.contains("\"SpotPrice\""));
    }

    @Test
    public void testBuildTemplateShouldNotHaveSpotPriceSpecifiedWhenItIsSetToFalse() {
        TemplateGroup templateGroup1 = new TemplateGroup();
        AwsTemplate awsTemplate = new AwsTemplate();
        awsTemplate.setInstanceType(InstanceType.C1Medium);
        awsTemplate.setSshLocation("0.0.0.0/0");
        awsTemplate.setName("awstemp1");
        awsTemplate.setVolumeCount(1);
        awsTemplate.setVolumeSize(100);
        awsTemplate.setVolumeType(VolumeType.Gp2);
        templateGroup1.setNodeCount(1);
        templateGroup1.setTemplate(awsTemplate);
        templateGroup1.setGroupName("master");
        // WHEN
        String result = underTest.build("templates/aws-cf-stack.ftl", false, Arrays.asList(templateGroup1));
        // THEN
        assertFalse(result.contains("\"SpotPrice\""));
    }

    @Test(expected = InternalServerException.class)
    public void testBuildTemplateShouldThrowInternalServerExceptionWhenTemplateDoesNotExist() {
        // WHEN
        underTest.build("templates/non-existent.ftl", false, new ArrayList<TemplateGroup>());
    }
}
