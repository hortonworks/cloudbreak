package com.sequenceiq.cloudbreak.cmtemplate.configproviders.clo;

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

class CLOServiceConfigProviderTest {
    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Mock
    private TemplatePreparationObject source;

    @Mock
    private GeneralClusterConfigs generalClusterConfigs;

    private AutoCloseable closeable;

    @InjectMocks
    private CLOServiceConfigProvider cloServiceConfigProvider;

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
        assertEquals(CLOServiceRoles.CLO_SERVICE, cloServiceConfigProvider.getServiceType());
    }

    @Test
    void testGetRoleTypes() {
        assertEquals(List.of(CLOServiceRoles.CLO_SERVER), cloServiceConfigProvider.getRoleTypes());
    }

    @Test
    void testIsConfigurationNeeded() {
        assertTrue(cloServiceConfigProvider.isConfigurationNeeded(cmTemplateProcessor, source));
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

        List<ApiClusterTemplateConfig> configs = cloServiceConfigProvider.getServiceConfigs(cmTemplateProcessor, source);

        assertEquals(4, configs.size());
        assertEquals(CLOServiceConfigProvider.CLO_DATAHUB_ENVIRONMENT_CRN, configs.getFirst().getName());
        assertEquals(environmentCrn, configs.getFirst().getValue());

        assertEquals(CLOServiceConfigProvider.CLO_DATAHUB_RESOURCE_CRN, configs.get(1).getName());
        assertEquals(resourceCrn, configs.get(1).getValue());

        assertEquals(CLOServiceConfigProvider.CLO_ACCOUNT_ID, configs.get(2).getName());
        assertEquals(accountId, configs.get(2).getValue());

        assertEquals(CLOServiceConfigProvider.CLO_CLOUD_PROVIDER, configs.get(3).getName());
        assertEquals(cloudProvider, configs.get(3).getValue());
    }

    @Test
    void testGetServiceConfigsWhenAccountIdIsEmpty() {
        String resourceCrn = "test-resource-crn";

        when(generalClusterConfigs.getResourceCrn()).thenReturn(resourceCrn);
        when(generalClusterConfigs.getAccountId()).thenReturn(Optional.empty());
        when(source.getCloudPlatform()).thenReturn(CloudPlatform.AWS);

        List<ApiClusterTemplateConfig> configs = cloServiceConfigProvider.getServiceConfigs(cmTemplateProcessor, source);

        assertEquals(4, configs.size());
        assertEquals("UNKNOWN", configs.get(2).getValue());
    }
}