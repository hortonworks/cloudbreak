package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.domain.AwsInstanceType;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.AwsVolumeType;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Stack;

import freemarker.template.Configuration;

public class CloudFormationTemplateBuilderTest {

    @InjectMocks
    private CloudFormationTemplateBuilder underTest;

    @Mock
    private Stack stack;

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
        List<InstanceGroup> instanceGroupsList = Arrays.asList(instanceGroup1);
        Set<InstanceGroup> instanceGroupSet = new HashSet<>(instanceGroupsList);
        when(stack.getInstanceGroupsAsList()).thenReturn(instanceGroupsList);
        when(stack.getInstanceGroups()).thenReturn(instanceGroupSet);
        // WHEN
        String result = underTest.build(stack, "templates/aws-cf-stack.ftl");
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
        List<InstanceGroup> instanceGroupsList = Arrays.asList(instanceGroup1);
        Set<InstanceGroup> instanceGroupSet = new HashSet<>(instanceGroupsList);
        when(stack.getInstanceGroupsAsList()).thenReturn(instanceGroupsList);
        when(stack.getInstanceGroups()).thenReturn(instanceGroupSet);
        // WHEN
        String result = underTest.build(stack, "templates/aws-cf-stack.ftl");
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
        List<InstanceGroup> instanceGroupsList = Arrays.asList(instanceGroup1);
        Set<InstanceGroup> instanceGroupSet = new HashSet<>(instanceGroupsList);
        when(stack.getInstanceGroupsAsList()).thenReturn(instanceGroupsList);
        when(stack.getInstanceGroups()).thenReturn(instanceGroupSet);
        // WHEN
        String result = underTest.build(stack, "templates/aws-cf-stack.ftl");
        // THEN
        assertFalse(result.contains("\"SpotPrice\""));
    }

    @Test(expected = InternalServerException.class)
    public void testBuildTemplateShouldThrowInternalServerExceptionWhenTemplateDoesNotExist() {
        // WHEN
        underTest.build(stack, "templates/non-existent.ftl");
    }
}
