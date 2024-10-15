package com.sequenceiq.cloudbreak.cmtemplate.configproviders.core;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_12_0_500;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

class CommonCoreConfigProviderTest {

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Mock
    private TemplatePreparationObject source;

    @Mock
    private GeneralClusterConfigs generalClusterConfigs;

    private AutoCloseable closeable;

    @InjectMocks
    private CommonCoreConfigProvider commonCoreConfigProvider;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        when(source.getGeneralClusterConfigs()).thenReturn(generalClusterConfigs);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (closeable != null) {
            closeable.close();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"7.12.0.500", "7.12.0.500-58279810", "7.12.0.600"})
    void testGetServiceConfigsShouldContainClusterSpecificProperties(String cmGbn) {
        String resourceCrn = "test-resource-crn";
        String accountId = "test-account-id";
        String environmentCrn = "test-environment-crn";
        String cloudProvider = "AWS";
        when(generalClusterConfigs.isGovCloud()).thenReturn(false);
        when(generalClusterConfigs.getResourceCrn()).thenReturn(resourceCrn);
        when(generalClusterConfigs.getAccountId()).thenReturn(Optional.of(accountId));
        when(generalClusterConfigs.getEnvironmentCrn()).thenReturn(environmentCrn);
        when(source.getCloudPlatform()).thenReturn(CloudPlatform.AWS);
        when(cmTemplateProcessor.getCmVersion()).thenReturn(Optional.of(cmGbn));
        List<ApiClusterTemplateConfig> serviceConfigs = commonCoreConfigProvider.getServiceConfigs(cmTemplateProcessor, source);
        assertEquals(4, serviceConfigs.size());
        assertEquals("environment_crn", serviceConfigs.get(0).getName());
        assertEquals(environmentCrn, serviceConfigs.get(0).getValue());
        assertEquals("resource_crn", serviceConfigs.get(1).getName());
        assertEquals(resourceCrn, serviceConfigs.get(1).getValue());
        assertEquals("environment_account_id", serviceConfigs.get(2).getName());
        assertEquals(accountId, serviceConfigs.get(2).getValue());
        assertEquals("environment_cloud_provider", serviceConfigs.get(3).getName());
        assertEquals(cloudProvider, serviceConfigs.get(3).getValue());
    }

    @Test
    void testGetServiceConfigsWhenAccountIdIsEmpty() {
        String resourceCrn = "test-resource-crn";

        when(generalClusterConfigs.getResourceCrn()).thenReturn(resourceCrn);
        when(generalClusterConfigs.getAccountId()).thenReturn(Optional.empty());
        when(source.getCloudPlatform()).thenReturn(CloudPlatform.AWS);
        when(cmTemplateProcessor.getCmVersion()).thenReturn(Optional.ofNullable(CLOUDERAMANAGER_VERSION_7_12_0_500.getVersion()));

        List<ApiClusterTemplateConfig> configs = commonCoreConfigProvider.getServiceConfigs(cmTemplateProcessor, source);

        assertEquals(4, configs.size());
        assertEquals("UNKNOWN", configs.get(2).getValue());
    }

    @Test
    void testIsConfigurationRequiredReturnsTrue() {
        assertTrue(commonCoreConfigProvider.isConfigurationNeeded(cmTemplateProcessor, source));
    }

    @Test
    void testFilterByHostGroupViewTypeReturnsCorrectPredicate() {
        assertTrue(commonCoreConfigProvider.filterByHostGroupViewType().test(mock(HostgroupView.class)));
        assertTrue(commonCoreConfigProvider.filterByHostGroupViewType().test(null));
    }

    @Test
    void testShouldNotReturnAnyClusterConfigIfCMVersionIsNotApplicable() {
        // anything less than 7.12.0.500 or empty is not applicable
        Set<String> cmVersions = Set.of("7.12.0", "7.11.0", "7.12.0.400", "");
        cmVersions.forEach(cmVersion -> {
            when(cmTemplateProcessor.getCmVersion()).thenReturn(Optional.ofNullable(cmVersion));
            List<ApiClusterTemplateConfig> serviceConfigs = commonCoreConfigProvider.getServiceConfigs(cmTemplateProcessor, source);
            assertEquals(0, serviceConfigs.size());
        });

    }
}