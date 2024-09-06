package com.sequenceiq.cloudbreak.cmtemplate.configproviders.dlm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

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

class DLMServiceConfigProviderTest {
    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Mock
    private TemplatePreparationObject source;

    @Mock
    private GeneralClusterConfigs generalClusterConfigs;

    private AutoCloseable closeable;

    @InjectMocks
    private DLMServiceConfigProvider dlmServiceConfigProvider;

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
    void testGetServiceType() {
        assertEquals(DLMServiceRoles.DLM_SERVICE, dlmServiceConfigProvider.getServiceType());
    }

    @Test
    void testGetRoleTypes() {
        assertEquals(List.of(DLMServiceRoles.DLM_SERVER), dlmServiceConfigProvider.getRoleTypes());
    }

    @Test
    void testIsConfigurationNeeded() {
        assertTrue(dlmServiceConfigProvider.isConfigurationNeeded(cmTemplateProcessor, source));
    }

    @Test
    void testGetServiceConfigs() {
        String resourceCrn = "test-resource-crn";
        String accountId = "test-account-id";
        String environmentCrn = "test-environment-crn";
        String cloudProvider = "AWS";

        when(generalClusterConfigs.getResourceCrn()).thenReturn(resourceCrn);
        when(generalClusterConfigs.getAccountId()).thenReturn(Optional.of(accountId));
        when(generalClusterConfigs.getEnvironmentCrn()).thenReturn(environmentCrn);
        when(source.getCloudPlatform()).thenReturn(CloudPlatform.AWS);

        List<ApiClusterTemplateConfig> configs = dlmServiceConfigProvider.getServiceConfigs(cmTemplateProcessor, source);

        assertEquals(4, configs.size());
        assertEquals(DLMServiceConfigProvider.DLM_DATAHUB_ENVIRONMENT_CRN, configs.getFirst().getName());
        assertEquals(environmentCrn, configs.getFirst().getValue());

        assertEquals(DLMServiceConfigProvider.DLM_DATAHUB_RESOURCE_CRN, configs.get(1).getName());
        assertEquals(resourceCrn, configs.get(1).getValue());

        assertEquals(DLMServiceConfigProvider.DLM_ACCOUNT_ID, configs.get(2).getName());
        assertEquals(accountId, configs.get(2).getValue());

        assertEquals(DLMServiceConfigProvider.DLM_CLOUD_PROVIDER, configs.get(3).getName());
        assertEquals(cloudProvider, configs.get(3).getValue());
    }

    @Test
    void testGetServiceConfigsWhenAccountIdIsEmpty() {
        String resourceCrn = "test-resource-crn";
        String cloudProvider = "AWS";

        when(generalClusterConfigs.getResourceCrn()).thenReturn(resourceCrn);
        when(generalClusterConfigs.getAccountId()).thenReturn(Optional.empty());
        when(source.getCloudPlatform()).thenReturn(CloudPlatform.AWS);

        List<ApiClusterTemplateConfig> configs = dlmServiceConfigProvider.getServiceConfigs(cmTemplateProcessor, source);

        assertEquals(4, configs.size());
        assertEquals("UNKNOWN", configs.get(2).getValue());
    }
}