package com.sequenceiq.cloudbreak.cmtemplate.configproviders.core;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_12_0_500;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

class CommonServiceConfigProviderTest {

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Mock
    private TemplatePreparationObject source;

    @Mock
    private GeneralClusterConfigs generalClusterConfigs;

    private AutoCloseable closeable;

    @InjectMocks
    private CommonServiceConfigProvider commonServiceConfigProvider;

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

    @Test
    void testGetServiceConfigsShouldContainClusterSpecificProperties() {
        String resourceCrn = "test-resource-crn";
        String accountId = "test-account-id";
        String environmentCrn = "test-environment-crn";
        String cloudProvider = "AWS";
        when(generalClusterConfigs.isGovCloud()).thenReturn(false);
        when(generalClusterConfigs.getResourceCrn()).thenReturn(resourceCrn);
        when(generalClusterConfigs.getAccountId()).thenReturn(Optional.of(accountId));
        when(generalClusterConfigs.getEnvironmentCrn()).thenReturn(environmentCrn);
        when(source.getCloudPlatform()).thenReturn(CloudPlatform.AWS);
        when(cmTemplateProcessor.getCmVersion()).thenReturn(Optional.ofNullable(CLOUDERAMANAGER_VERSION_7_12_0_500.getVersion()));
        List<ApiClusterTemplateConfig> serviceConfigs = commonServiceConfigProvider.getServiceConfigs(cmTemplateProcessor, source);
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
    public void testGetAdditionalServicesCallsCommonServiceConfigProviderHostGroupViewFilter() {
        Set<HostgroupView> hostgroupViews = mock(Set.class);
        when(source.getHostgroupViews()).thenReturn(hostgroupViews);
        when(cmTemplateProcessor.getServiceByType(anyString())).thenReturn(Optional.empty());
        when(hostgroupViews.stream()).thenReturn(mock(Stream.class));
        CommonServiceConfigProvider spyCommonServiceConfigProvider = spy(CommonServiceConfigProvider.class);
        spyCommonServiceConfigProvider.getAdditionalServices(cmTemplateProcessor, source);
        verify(spyCommonServiceConfigProvider).filterByHostGroupViewType();
    }
}