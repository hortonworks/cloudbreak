package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.azure.AzureManagedPrivateDnsZoneServiceType.POSTGRES;
import static com.sequenceiq.cloudbreak.cloud.azure.AzureManagedPrivateDnsZoneServiceType.POSTGRES_FLEXIBLE;
import static com.sequenceiq.cloudbreak.cloud.azure.AzureManagedPrivateDnsZoneServiceType.POSTGRES_FLEXIBLE_FOR_PRIVATE_ENDPOINT;
import static com.sequenceiq.cloudbreak.cloud.azure.AzureManagedPrivateDnsZoneServiceType.STORAGE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.JsonExpectationsHelper;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.cloud.azure.connector.resource.AzureDnsZoneDeploymentParameters;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;

import freemarker.template.TemplateException;

@ExtendWith(MockitoExtension.class)
public class AzureNetworkDnsZoneTemplateBuilderTest {

    @Mock
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @InjectMocks
    private AzureNetworkDnsZoneTemplateBuilder underTest;

    @BeforeEach
    public void before() throws IOException, TemplateException {
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        ReflectionTestUtils.setField(underTest, "freemarkerConfiguration", factoryBean.getObject());
        ReflectionTestUtils.setField(underTest, "armTemplatePath", "templates/arm-network-dnszone.ftl");
    }

    @Test
    public void whenBuildTemplateThenModelParametersAreSet() throws Exception {
        AzureDnsZoneDeploymentParameters parameters = new AzureDnsZoneDeploymentParameters("networkId", false,
                List.of(STORAGE, POSTGRES, POSTGRES_FLEXIBLE, POSTGRES_FLEXIBLE_FOR_PRIVATE_ENDPOINT),
                "resourceGroup",
                Collections.emptyMap());

        when(freeMarkerTemplateUtils.processTemplateIntoString(any(), any())).thenCallRealMethod();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode expectedJson = objectMapper.readTree(new File("src/test/resources/json/arm-network-dnszone.json"));
        String actual = underTest.build(parameters);
        new JsonExpectationsHelper().assertJsonEqual(expectedJson.toString(), actual, false);
    }
}