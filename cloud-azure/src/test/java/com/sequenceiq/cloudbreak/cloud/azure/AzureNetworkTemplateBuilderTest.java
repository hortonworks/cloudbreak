package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

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
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.common.service.DefaultCostTaggingService;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;

import freemarker.template.Template;
import freemarker.template.TemplateException;

@RunWith(MockitoJUnitRunner.class)
public class AzureNetworkTemplateBuilderTest {

    private static final String ENV_NAME = "testEnv";

    private static final String REGION = "US-WEST";

    private static final String NETWORK_CIDR = "1.1.1.1/8";

    @InjectMocks
    private AzureNetworkTemplateBuilder underTest;

    @Mock
    private DefaultCostTaggingService defaultCostTaggingService;

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
        ReflectionTestUtils.setField(underTest, "armTemplateParametersPath", "templates/parameters.ftl");
    }

    @Test
    public void testBuildShouldReturnTheRenderedTemplate() throws IOException, TemplateException {
        NetworkCreationRequest networkCreationRequest = createRequest();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode expectedJson = objectMapper.readTree(new File("src/test/resources/json/arm-network.json"));

        when(defaultCostTaggingService.prepareNetworkTagging()).thenReturn(Collections.emptyMap());
        when(freeMarkerTemplateUtils.processTemplateIntoString(any(), any())).thenCallRealMethod();

        String actual = underTest.build(networkCreationRequest);

        JsonNode json = objectMapper.readTree(actual);
        assertEquals(expectedJson, json);
        verify(freeMarkerTemplateUtils).processTemplateIntoString(any(Template.class), anyMap());
        verify(defaultCostTaggingService).prepareNetworkTagging();
    }

    private NetworkCreationRequest createRequest() {
        return new NetworkCreationRequest.Builder()
                .withEnvName(ENV_NAME)
                .withRegion(Region.region(REGION))
                .withNetworkCidr(NETWORK_CIDR)
                .withSubnetCidrs(createSubnetCidrs())
                .withNoPublicIp(false)
                .withNoFirewallRules(false)
                .withId(1L)
                .build();
    }

    private Set<String> createSubnetCidrs() {
        Set<String> subnetCidrs = new TreeSet();
        subnetCidrs.add("2.2.2.2/24");
        subnetCidrs.add("3.3.3.3/24");
        return subnetCidrs;
    }
}