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

import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.domain.AwsInstanceType;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.AwsVolumeType;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;

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
        InstanceGroup instanceGroup1 = new InstanceGroup();
        AwsTemplate awsTemplate = new AwsTemplate();
        awsTemplate.setInstanceType(AwsInstanceType.C3Large);
        awsTemplate.setSpotPrice(0.2);
        awsTemplate.setSshLocation("0.0.0.0/0");
        awsTemplate.setName("awstemp1");
        awsTemplate.setVolumeCount(2);
        awsTemplate.setVolumeSize(100);
        awsTemplate.setVolumeType(AwsVolumeType.Gp2);
        instanceGroup1.setNodeCount(1);
        instanceGroup1.setTemplate(awsTemplate);
        instanceGroup1.setGroupName("master");
        // WHEN
        String result = underTest.build("templates/aws-cf-stack.ftl", false, Arrays.asList(instanceGroup1));
        // THEN
        assertTrue(result.contains("\"DeviceName\" : \"/dev/xvdf\""));
        assertTrue(result.contains("\"DeviceName\" : \"/dev/xvdg\""));
        assertFalse(result.contains("\"DeviceName\" : \"/dev/xvdh\""));
    }

    @Test
    public void testBuildTemplateShouldHaveSpotPriceSpecifiedWhenItIsSet() {
        InstanceGroup instanceGroup1 = new InstanceGroup();
        AwsTemplate awsTemplate = new AwsTemplate();
        awsTemplate.setInstanceType(AwsInstanceType.C3Large);
        awsTemplate.setSpotPrice(0.2);
        awsTemplate.setSshLocation("0.0.0.0/0");
        awsTemplate.setName("awstemp1");
        awsTemplate.setVolumeCount(1);
        awsTemplate.setVolumeSize(100);
        awsTemplate.setVolumeType(AwsVolumeType.Gp2);
        instanceGroup1.setNodeCount(1);
        instanceGroup1.setTemplate(awsTemplate);
        instanceGroup1.setGroupName("master");
        // WHEN
        String result = underTest.build("templates/aws-cf-stack.ftl", true, Arrays.asList(instanceGroup1));
        // THEN
        assertTrue(result.contains("\"SpotPrice\""));
    }

    @Test
    public void testBuildTemplateShouldNotHaveSpotPriceSpecifiedWhenItIsSetToFalse() {
        InstanceGroup instanceGroup1 = new InstanceGroup();
        AwsTemplate awsTemplate = new AwsTemplate();
        awsTemplate.setInstanceType(AwsInstanceType.C3Large);
        awsTemplate.setSshLocation("0.0.0.0/0");
        awsTemplate.setName("awstemp1");
        awsTemplate.setVolumeCount(1);
        awsTemplate.setVolumeSize(100);
        awsTemplate.setVolumeType(AwsVolumeType.Gp2);
        instanceGroup1.setNodeCount(1);
        instanceGroup1.setTemplate(awsTemplate);
        instanceGroup1.setGroupName("master");
        // WHEN
        String result = underTest.build("templates/aws-cf-stack.ftl", false, Arrays.asList(instanceGroup1));
        // THEN
        assertFalse(result.contains("\"SpotPrice\""));
    }

    @Test(expected = InternalServerException.class)
    public void testBuildTemplateShouldThrowInternalServerExceptionWhenTemplateDoesNotExist() {
        // WHEN
        underTest.build("templates/non-existent.ftl", false, new ArrayList<InstanceGroup>());
    }
}
