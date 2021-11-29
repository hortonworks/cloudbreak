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
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.config.ConversionConfig;
import com.sequenceiq.cloudbreak.converter.StackToTemplatePreparationObjectConverter;
import com.sequenceiq.cloudbreak.converter.v4.clustertemplate.DefaultClusterTemplateV4RequestToClusterTemplateConverter;
import com.sequenceiq.cloudbreak.init.clustertemplate.DefaultClusterTemplateCache;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.template.ClusterTemplateService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.distrox.v1.distrox.service.InternalClusterTemplateValidator;

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
        assertNotNull(azureSupportedTypes);
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
            ConversionConfig.class})
    @PropertySource("classpath:application.yml")
    static class TestAppContext {

        @MockBean
        private ClusterTemplateService clusterTemplateService;

        @MockBean
        private DefaultClusterTemplateV4RequestToClusterTemplateConverter converter;

        @MockBean
        private StackToTemplatePreparationObjectConverter mockPreparationObject;

        @MockBean
        private WorkspaceService workspaceService;

        @MockBean
        private UserService userService;

        @MockBean
        private CloudbreakRestRequestThreadLocalService cloudbreakRestRequestThreadLocalService;

        @MockBean
        private BlueprintService blueprintService;

        @MockBean
        private EntitlementService entitlementService;

        @MockBean
        private InternalClusterTemplateValidator internalClusterTemplateValidator;

    }
}
