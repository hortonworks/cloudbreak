package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.model.network.SubnetType.PUBLIC;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

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
import com.google.common.collect.Lists;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkSubnetRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetRequest;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;

import freemarker.template.Template;
import freemarker.template.TemplateException;

@RunWith(MockitoJUnitRunner.class)
public class AzureNetworkTemplateBuilderTest {

    private static final String ENV_NAME = "testEnv";

    private static final String RG_NAME = "testRg";

    private static final String STACK_NAME = "testEnv-1";

    private static final String REGION = "US-WEST";

    private static final String NETWORK_CIDR = "1.1.1.1/8";

    private static final String ENV_CRN = "someCrn";

    @InjectMocks
    private AzureNetworkTemplateBuilder underTest;

    @Mock
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @Before
    public void before() throws IOException, TemplateException {
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        ReflectionTestUtils.setField(underTest, "freemarkerConfiguration", factoryBean.getObject());
        ReflectionTestUtils.setField(underTest, "armTemplatePath", "templates/arm-network.ftl");
    }

    @Test
    public void testBuildShouldReturnTheRenderedTemplate() throws IOException, TemplateException {
        NetworkCreationRequest networkCreationRequest = createRequest();
        ResourceGroup resourceGroup = mock(ResourceGroup.class);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode expectedJson = objectMapper.readTree(new File("src/test/resources/json/arm-network.json"));

        when(freeMarkerTemplateUtils.processTemplateIntoString(any(), any())).thenCallRealMethod();
        when(resourceGroup.name()).thenReturn("testRg");

        String actual = underTest.build(networkCreationRequest, Lists.newArrayList(
                publicSubnetRequest("10.0.1.0/24", 0),
                publicSubnetRequest("10.0.1.0/24", 1),
                publicSubnetRequest("10.0.1.0/24", 2)),
                resourceGroup.name());

        JsonNode json = objectMapper.readTree(actual);
        assertEquals(expectedJson, json);
        verify(freeMarkerTemplateUtils).processTemplateIntoString(any(Template.class), anyMap());
    }

    private NetworkCreationRequest createRequest() {
        return new NetworkCreationRequest.Builder()
                .withEnvName(ENV_NAME)
                .withEnvCrn(ENV_CRN)
                .withRegion(Region.region(REGION))
                .withNetworkCidr(NETWORK_CIDR)
                .withPublicSubnets(createSubnets())
                .withPrivateSubnets(createSubnets())
                .withNoPublicIp(false)
                .withStackName(STACK_NAME)
                .withResourceGroup(RG_NAME)
                .build();
    }

    private Set<NetworkSubnetRequest> createSubnets() {
        Set<NetworkSubnetRequest> subnets = new HashSet<>();
        subnets.add(createSubnetRequest("2.2.2.2/24"));
        subnets.add(createSubnetRequest("3.3.3.3/24"));
        return subnets;
    }

    public SubnetRequest publicSubnetRequest(String cidr, int index) {
        SubnetRequest subnetRequest = new SubnetRequest();
        subnetRequest.setIndex(index);
        subnetRequest.setPublicSubnetCidr(cidr);
        subnetRequest.setSubnetGroup(index % 3);
        subnetRequest.setAvailabilityZone("az");
        return subnetRequest;
    }

    private NetworkSubnetRequest createSubnetRequest(String s) {
        return new NetworkSubnetRequest(s, PUBLIC);
    }
}