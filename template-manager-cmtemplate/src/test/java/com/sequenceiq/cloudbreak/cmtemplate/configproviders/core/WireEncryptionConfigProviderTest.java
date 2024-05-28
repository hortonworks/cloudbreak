package com.sequenceiq.cloudbreak.cmtemplate.configproviders.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;

@ExtendWith(MockitoExtension.class)
public class WireEncryptionConfigProviderTest {
    private static final String TEST_USER_CRN = "crn:cdp:iam:us-west-1:accid:user:mockuser@cloudera.com";

    @InjectMocks
    private WireEncryptionConfigProvider underTest;

    @Mock
    private EntitlementService entitlementService;

    @Test
    public void isConfigurationNeededWhenNotPresentedHdfsNotAndStorageConfiguredMustReturnTrueWireEncryptionEnabled() {
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject templatePreparationObject = mock(TemplatePreparationObject.class);
        GeneralClusterConfigs generalClusterConfigs = mock(GeneralClusterConfigs.class);

        when(entitlementService.isWireEncryptionEnabled(anyString())).thenReturn(true);
        when(templatePreparationObject.getStackType()).thenReturn(StackType.DATALAKE);
        when(generalClusterConfigs.isGovCloud()).thenReturn(false);
        when(templatePreparationObject.getGeneralClusterConfigs()).thenReturn(generalClusterConfigs);

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(mockTemplateProcessor, templatePreparationObject);
            Map<String, ApiClusterTemplateService> additionalServices = underTest.getAdditionalServices(mockTemplateProcessor, templatePreparationObject);
            List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(mockTemplateProcessor, templatePreparationObject);
            assertTrue(underTest.isConfigurationNeeded(mockTemplateProcessor, templatePreparationObject));
            assertEquals(0, roleConfigs.size());
            assertEquals(0, additionalServices.size());
            assertEquals(1, serviceConfigs.size());
            assertEquals("hadoop_rpc_protection", serviceConfigs.get(0).getName());
            assertEquals("privacy", serviceConfigs.get(0).getValue());
        });
    }

    @Test
    public void isConfigurationNeededWhenNotPresentedHdfsNotAndStorageConfiguredAndGovCloudMustReturnTrue() {
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject templatePreparationObject = mock(TemplatePreparationObject.class);
        GeneralClusterConfigs generalClusterConfigs = mock(GeneralClusterConfigs.class);

        when(generalClusterConfigs.isGovCloud()).thenReturn(true);
        when(templatePreparationObject.getGeneralClusterConfigs()).thenReturn(generalClusterConfigs);

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(mockTemplateProcessor, templatePreparationObject);
            Map<String, ApiClusterTemplateService> additionalServices = underTest.getAdditionalServices(mockTemplateProcessor, templatePreparationObject);
            List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(mockTemplateProcessor, templatePreparationObject);
            assertTrue(underTest.isConfigurationNeeded(mockTemplateProcessor, templatePreparationObject));
            assertEquals(0, roleConfigs.size());
            assertEquals(0, additionalServices.size());
            assertEquals(1, serviceConfigs.size());
            assertEquals("hadoop_rpc_protection", serviceConfigs.get(0).getName());
            assertEquals("privacy", serviceConfigs.get(0).getValue());
        });
    }

    @Test
    public void isConfigurationNotNeededWhenNotPresentedHdfsNotAndStorageConfiguredAndDefaultFsNotConfiguredMustReturnFalseWireEncryptionEnabled() {
        when(entitlementService.isWireEncryptionEnabled(anyString())).thenReturn(true);
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject templatePreparationObject = mock(TemplatePreparationObject.class);
        GeneralClusterConfigs generalClusterConfigs = mock(GeneralClusterConfigs.class);

        when(templatePreparationObject.getStackType()).thenReturn(StackType.DATALAKE);
        when(generalClusterConfigs.isGovCloud()).thenReturn(false);
        when(templatePreparationObject.getGeneralClusterConfigs()).thenReturn(generalClusterConfigs);

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(mockTemplateProcessor, templatePreparationObject);
            Map<String, ApiClusterTemplateService> additionalServices = underTest.getAdditionalServices(mockTemplateProcessor, templatePreparationObject);
            List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(mockTemplateProcessor, templatePreparationObject);
            assertTrue(underTest.isConfigurationNeeded(mockTemplateProcessor, templatePreparationObject));
            assertEquals(0, roleConfigs.size());
            assertEquals(Map.of(), additionalServices);
            assertEquals(1, serviceConfigs.size());
            assertEquals("hadoop_rpc_protection", serviceConfigs.get(0).getName());
            assertEquals("privacy", serviceConfigs.get(0).getValue());
        });
    }

    @Test
    public void isConfigurationNeededWhenKafkaPresentedHdfsNotAndStorageNotConfiguredMustReturnFalseWireEncryptionEnabled() {
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject templatePreparationObject = mock(TemplatePreparationObject.class);
        GeneralClusterConfigs generalClusterConfigs = mock(GeneralClusterConfigs.class);

        when(entitlementService.isWireEncryptionEnabled(anyString())).thenReturn(true);
        when(templatePreparationObject.getStackType()).thenReturn(StackType.DATALAKE);
        when(generalClusterConfigs.isGovCloud()).thenReturn(false);
        when(templatePreparationObject.getGeneralClusterConfigs()).thenReturn(generalClusterConfigs);

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(mockTemplateProcessor, templatePreparationObject);
            Map<String, ApiClusterTemplateService> additionalServices = underTest.getAdditionalServices(mockTemplateProcessor, templatePreparationObject);
            List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(mockTemplateProcessor, templatePreparationObject);
            assertTrue(underTest.isConfigurationNeeded(mockTemplateProcessor, templatePreparationObject));
            assertEquals(0, roleConfigs.size());
            assertEquals(Map.of(), additionalServices);
            assertEquals(1, serviceConfigs.size());
            assertEquals("hadoop_rpc_protection", serviceConfigs.get(0).getName());
            assertEquals("privacy", serviceConfigs.get(0).getValue());
        });
    }
}
