package com.sequenceiq.cloudbreak.cmtemplate.configproviders.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.TrustView;
import com.sequenceiq.cloudbreak.sdx.RdcView;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.DatalakeView;

class HybridCoreConfigProviderTest {

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Mock
    private TemplatePreparationObject source;

    @InjectMocks
    private HybridCoreConfigProvider underTest;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @Test
    void getServiceConfigsAddsRpcProtectionAndTrustedRealms() {
        DatalakeView datalakeView = mock(DatalakeView.class);
        RdcView rdcView = mock(RdcView.class);
        TrustView trustView = mock(TrustView.class);
        KerberosConfig kerberosConfig = mock(KerberosConfig.class);

        when(source.getDatalakeView()).thenReturn(Optional.of(datalakeView));
        when(datalakeView.getRdcView()).thenReturn(rdcView);
        when(rdcView.getServiceConfig("HIVE", "hadoop_rpc_protection")).thenReturn("privacy");
        when(source.getTrustView()).thenReturn(Optional.of(trustView));
        when(trustView.realm()).thenReturn("realm1");
        when(source.getKerberosConfig()).thenReturn(Optional.of(kerberosConfig));
        when(kerberosConfig.getRealm()).thenReturn("realm2");

        List<ApiClusterTemplateConfig> configs = underTest.getServiceConfigs(cmTemplateProcessor, source);

        assertEquals(3, configs.size());
        assertEquals("hadoop_rpc_protection", configs.get(0).getName());
        assertEquals("privacy", configs.get(0).getValue());
        assertEquals("trusted_realms", configs.get(1).getName());
        assertEquals("REALM1,realm2", configs.get(1).getValue());
        assertEquals("set_auth_to_local_to_lowercase", configs.get(2).getName());
        assertEquals("true", configs.get(2).getValue());
    }

    @Test
    void getServiceConfigsNoTrustedRealmsNoRpcProtection() {
        when(source.getDatalakeView()).thenReturn(Optional.empty());
        when(source.getTrustView()).thenReturn(Optional.empty());
        when(source.getKerberosConfig()).thenReturn(Optional.empty());

        List<ApiClusterTemplateConfig> configs = underTest.getServiceConfigs(cmTemplateProcessor, source);

        assertTrue(configs.isEmpty());
    }

    @Test
    void getServiceConfigsOnlyRpcProtection() {
        DatalakeView datalakeView = mock(DatalakeView.class);
        RdcView rdcView = mock(RdcView.class);

        when(source.getDatalakeView()).thenReturn(Optional.of(datalakeView));
        when(datalakeView.getRdcView()).thenReturn(rdcView);
        when(rdcView.getServiceConfig("HIVE", "hadoop_rpc_protection")).thenReturn("integrity");
        when(source.getTrustView()).thenReturn(Optional.empty());
        when(source.getKerberosConfig()).thenReturn(Optional.empty());

        List<ApiClusterTemplateConfig> configs = underTest.getServiceConfigs(cmTemplateProcessor, source);

        assertEquals(1, configs.size());
        assertEquals("hadoop_rpc_protection", configs.get(0).getName());
        assertEquals("integrity", configs.get(0).getValue());
    }

    @Test
    void getServiceConfigsOnlyTrustedRealms() {
        when(source.getDatalakeView()).thenReturn(Optional.empty());
        TrustView trustView = mock(TrustView.class);
        when(source.getTrustView()).thenReturn(Optional.of(trustView));
        when(trustView.realm()).thenReturn("realmX");
        when(source.getKerberosConfig()).thenReturn(Optional.empty());

        List<ApiClusterTemplateConfig> configs = underTest.getServiceConfigs(cmTemplateProcessor, source);

        assertEquals(2, configs.size());
        assertEquals("trusted_realms", configs.get(0).getName());
        assertEquals("REALMX", configs.get(0).getValue());
        assertEquals("set_auth_to_local_to_lowercase", configs.get(1).getName());
        assertEquals("true", configs.get(1).getValue());
    }

    @Test
    void isConfigurationNeededDelegatesToCmTemplateProcessor() {
        when(cmTemplateProcessor.isHybridDatahub(source)).thenReturn(true);
        assertTrue(underTest.isConfigurationNeeded(cmTemplateProcessor, source));
        when(cmTemplateProcessor.isHybridDatahub(source)).thenReturn(false);
        assertFalse(underTest.isConfigurationNeeded(cmTemplateProcessor, source));
    }
}
