package com.sequenceiq.cloudbreak.cloud.aws;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetRequest;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;

import freemarker.template.Template;
import freemarker.template.TemplateException;

@RunWith(MockitoJUnitRunner.class)
public class AwsNetworkCfTemplateProviderTest {

    private static final String VPC_CIDR = "0.0.0.0/16";

    private static final String TEMPLATE_PATH = "templates/aws-cf-network.ftl";

    @InjectMocks
    private AwsNetworkCfTemplateProvider underTest;

    @Mock
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    private List<SubnetRequest> subnetRequestList = createSubnetRequestList();

    @Before
    public void before() throws IOException, TemplateException {
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        ReflectionTestUtils.setField(underTest, "freemarkerConfiguration", factoryBean.getObject());
        ReflectionTestUtils.setField(underTest, "cloudFormationNetworkTemplatePath", TEMPLATE_PATH);
    }

    @Test
    public void testProvideShouldReturnTheTemplateWhenPrivateSubnetCreationEnabled() throws IOException, TemplateException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode expectedJson = objectMapper.readTree(new File("src/test/resources/json/aws-cf-network-private-subnet.json"));


        when(freeMarkerTemplateUtils.processTemplateIntoString(any(), any())).thenCallRealMethod();

        String actual = underTest.provide("envName", 1L, VPC_CIDR, subnetRequestList, true);

        JsonNode json = objectMapper.readTree(actual);
        assertEquals(expectedJson, json);
        verify(freeMarkerTemplateUtils).processTemplateIntoString(any(Template.class), anyMap());
    }

    @Test
    public void testProvideShouldReturnTheTemplateWhenPrivateSubnetCreationDisabled() throws IOException, TemplateException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode expectedJson = objectMapper.readTree(new File("src/test/resources/json/aws-cf-network.json"));


        when(freeMarkerTemplateUtils.processTemplateIntoString(any(), any())).thenCallRealMethod();

        String actual = underTest.provide("envName", 1L, VPC_CIDR, subnetRequestList, false);

        JsonNode json = objectMapper.readTree(actual);
        assertEquals(expectedJson, json);
        verify(freeMarkerTemplateUtils).processTemplateIntoString(any(Template.class), anyMap());
    }

    @Test(expected = CloudConnectorException.class)
    public void testProvideShouldThrowExceptionWhenTemplateProcessHasFailed() throws IOException, TemplateException {
        when(freeMarkerTemplateUtils.processTemplateIntoString(any(Template.class), anyMap())).thenThrow(TemplateException.class);

        underTest.provide("envName", 1L, VPC_CIDR, subnetRequestList, true);

        verify(freeMarkerTemplateUtils).processTemplateIntoString(any(Template.class), anyMap());
    }

    private List<SubnetRequest> createSubnetRequestList() {
        SubnetRequest subnetRequest1 = new SubnetRequest();
        subnetRequest1.setPublicSubnetCidr("2.2.2.2/24");
        subnetRequest1.setPrivateSubnetCidr("2.2.2.2/24");
        subnetRequest1.setIndex(0);
        subnetRequest1.setSubnetGroup(0);
        subnetRequest1.setAvailabilityZone("az1");

        SubnetRequest subnetRequest2 = new SubnetRequest();
        subnetRequest2.setPublicSubnetCidr("2.2.2.2/24");
        subnetRequest2.setPrivateSubnetCidr("2.2.2.2/24");
        subnetRequest2.setIndex(1);
        subnetRequest2.setSubnetGroup(1);
        subnetRequest2.setAvailabilityZone("az2");

        return List.of(subnetRequest1, subnetRequest2);
    }
}