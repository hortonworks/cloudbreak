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
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
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

    private List<CreatedSubnet> createdSubnetList = createCloudSubnetList();

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
    public void testProvideShouldReturnTheTemplate() throws IOException, TemplateException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode expectedJson = objectMapper.readTree(new File("src/test/resources/json/aws-cf-network.json"));


        when(freeMarkerTemplateUtils.processTemplateIntoString(any(), any())).thenCallRealMethod();

        String actual = underTest.provide(VPC_CIDR, createdSubnetList);

        JsonNode json = objectMapper.readTree(actual);
        assertEquals(expectedJson, json);
        verify(freeMarkerTemplateUtils).processTemplateIntoString(any(Template.class), anyMap());
    }

    @Test(expected = CloudConnectorException.class)
    public void testProvideShouldThrowExceptionWhenTemplateProcessHasFailed() throws IOException, TemplateException {
        when(freeMarkerTemplateUtils.processTemplateIntoString(any(Template.class), anyMap())).thenThrow(TemplateException.class);

        underTest.provide(VPC_CIDR, createdSubnetList);

        verify(freeMarkerTemplateUtils).processTemplateIntoString(any(Template.class), anyMap());
    }

    private List<CreatedSubnet> createCloudSubnetList() {
        CreatedSubnet createdSubnet1 = new CreatedSubnet();
        createdSubnet1.setCidr("2.2.2.2/24");
        createdSubnet1.setAvailabilityZone("az1");

        CreatedSubnet createdSubnet2 = new CreatedSubnet();
        createdSubnet2.setCidr("2.2.2.2/24");
        createdSubnet2.setAvailabilityZone("az1");

        return List.of(createdSubnet1, createdSubnet2);
    }
}