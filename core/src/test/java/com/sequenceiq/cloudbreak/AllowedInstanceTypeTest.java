package com.sequenceiq.cloudbreak;

import static com.sequenceiq.cloudbreak.cloud.aws.common.DistroxEnabledInstanceTypes.AWS_ENABLED_ARM64_TYPES_LIST;
import static com.sequenceiq.cloudbreak.cloud.aws.common.DistroxEnabledInstanceTypes.AWS_ENABLED_X86_TYPES_LIST;
import static com.sequenceiq.cloudbreak.cloud.azure.DistroxEnabledInstanceTypes.AZURE_ENABLED_TYPES_LIST;
import static com.sequenceiq.cloudbreak.cloud.gcp.DistroxEnabledInstanceTypes.GCP_ENABLED_TYPES_LIST;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.DefaultClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.base64.Base64Util;
import com.sequenceiq.cloudbreak.common.gov.CommonGovService;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.provider.ProviderPreferencesService;
import com.sequenceiq.cloudbreak.config.ConversionConfig;
import com.sequenceiq.cloudbreak.converter.StackToTemplatePreparationObjectConverter;
import com.sequenceiq.cloudbreak.converter.v4.clustertemplate.DefaultClusterTemplateV4RequestToClusterTemplateConverter;
import com.sequenceiq.cloudbreak.init.clustertemplate.DefaultClusterTemplateCache;
import com.sequenceiq.cloudbreak.service.account.PreferencesService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.template.ClusterTemplateService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.distrox.v1.distrox.service.InternalClusterTemplateValidator;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = AllowedInstanceTypeTest.TestAppContext.class)
class AllowedInstanceTypeTest {

    @Inject
    private DefaultClusterTemplateCache templateCache;

    @Test
    void validateAwsClusterTemplatesByInstanceType() {
        Map<String, Pair<DefaultClusterTemplateV4Request, String>> stringClusterTemplateMap = templateCache.defaultClusterTemplateRequests();
        Set<String> allowedAwsTypes = new HashSet<>();
        allowedAwsTypes.addAll(AWS_ENABLED_X86_TYPES_LIST);
        allowedAwsTypes.addAll(AWS_ENABLED_ARM64_TYPES_LIST);
        stringClusterTemplateMap.entrySet()
                .stream()
                .map(ct -> templateCache.getDefaultClusterTemplate(Base64Util.decode(ct.getValue().getValue())))
                .filter(ct -> CloudPlatform.AWS.name().equalsIgnoreCase(ct.getCloudPlatform()))
                .forEach(ctr -> validateClusterTemplate(ctr, allowedAwsTypes));
        assertNotNull(AWS_ENABLED_X86_TYPES_LIST);
    }

    @Test
    void validateAzureClusterTemplatesByInstanceType() {
        Map<String, Pair<DefaultClusterTemplateV4Request, String>> stringClusterTemplateMap = templateCache.defaultClusterTemplateRequests();
        stringClusterTemplateMap.entrySet()
                .stream()
                .map(ct -> templateCache.getDefaultClusterTemplate(Base64Util.decode(ct.getValue().getValue())))
                .filter(ct -> CloudPlatform.AZURE.name().equalsIgnoreCase(ct.getCloudPlatform()))
                .forEach(ctr -> validateClusterTemplate(ctr, AZURE_ENABLED_TYPES_LIST));
        assertNotNull(AZURE_ENABLED_TYPES_LIST);
    }

    @Test
    void validateGcpClusterTemplatesByInstanceType() {
        Map<String, Pair<DefaultClusterTemplateV4Request, String>> stringClusterTemplateMap = templateCache.defaultClusterTemplateRequests();
        stringClusterTemplateMap.entrySet()
                .stream()
                .map(ct -> templateCache.getDefaultClusterTemplate(Base64Util.decode(ct.getValue().getValue())))
                .filter(ct -> CloudPlatform.GCP.name().equalsIgnoreCase(ct.getCloudPlatform()))
                .forEach(ctr -> validateClusterTemplate(ctr, GCP_ENABLED_TYPES_LIST));
        assertNotNull(GCP_ENABLED_TYPES_LIST);
    }

    private void validateClusterTemplate(DefaultClusterTemplateV4Request clusterTemplate, Collection<String> supportedTypes) {
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
        private PreferencesService preferencesService;

        @MockBean
        private CloudbreakRestRequestThreadLocalService cloudbreakRestRequestThreadLocalService;

        @MockBean
        private BlueprintService blueprintService;

        @MockBean
        private EntitlementService entitlementService;

        @MockBean
        private ProviderPreferencesService providerPreferencesService;

        @MockBean
        private CommonGovService commonGovService;

        @MockBean
        private InternalClusterTemplateValidator internalClusterTemplateValidator;

    }
}
