package com.sequenceiq.cloudbreak;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.DefaultClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.config.ConversionConfig;
import com.sequenceiq.cloudbreak.converter.StackToTemplatePreparationObjectConverter;
import com.sequenceiq.cloudbreak.converter.v4.clustertemplate.DefaultClusterTemplateV4RequestToClusterTemplateConverter;
import com.sequenceiq.cloudbreak.init.clustertemplate.DefaultClusterTemplateCache;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = AllowedInstanceTypeTest.TestAppContext.class)
public class AllowedInstanceTypeTest {

    @Value("${cb.aws.distrox.enabled.instance.types:}")
    private List<String> awsSupportedTypes;

    @Value("${cb.azure.distrox.enabled.instance.types:}")
    private List<String> azureSupportedTypes;

    @Inject
    private DefaultClusterTemplateCache templateCache;

    @Test
    public void validateAwsClusterTemplatesByInstanceType() {
        Map<String, String> stringClusterTemplateMap = templateCache.defaultClusterTemplateRequests();
        stringClusterTemplateMap.entrySet()
                .stream()
                .map(ct -> templateCache.getDefaultClusterTemplate(new String(Base64.getDecoder().decode(ct.getValue()))))
                .filter(ct -> CloudPlatform.AWS.name().equalsIgnoreCase(ct.getCloudPlatform()))
                .forEach(ctr -> validateClusterTemplate(ctr, awsSupportedTypes));
        assertNotNull(awsSupportedTypes);
    }

    @Test
    public void validateAzureClusterTemplatesByInstanceType() {
        Map<String, String> stringClusterTemplateMap = templateCache.defaultClusterTemplateRequests();
        stringClusterTemplateMap.entrySet()
                .stream()
                .map(ct -> templateCache.getDefaultClusterTemplate(new String(Base64.getDecoder().decode(ct.getValue()))))
                .filter(ct -> CloudPlatform.AZURE.name().equalsIgnoreCase(ct.getCloudPlatform()))
                .forEach(ctr -> validateClusterTemplate(ctr, azureSupportedTypes));
        assertNotNull(awsSupportedTypes);
    }

    private void validateClusterTemplate(DefaultClusterTemplateV4Request clusterTemplate, List<String> supportedTypes) {
        clusterTemplate.getDistroXTemplate().getInstanceGroups().stream()
                .filter(ig -> !supportedTypes.contains(ig.getTemplate().getInstanceType()))
                .findFirst()
                .ifPresent(ig -> {
                    fail(String.format("Template { %s } has invalid instance type { %s } in hostgroup { %s } added to it ",
                            clusterTemplate.getName(),
                            ig.getTemplate().getInstanceType(),
                            ig.getName()));
                });
    }

    @Configuration
    @ComponentScan(basePackageClasses = {DefaultClusterTemplateCache.class,
            ConversionConfig.class,
            ConverterUtil.class})
    @PropertySource("classpath:application.yml")
    static class TestAppContext {

        @MockBean
        private DefaultClusterTemplateV4RequestToClusterTemplateConverter converter;

        @MockBean
        private StackToTemplatePreparationObjectConverter mockPreparationObject;

    }
}
