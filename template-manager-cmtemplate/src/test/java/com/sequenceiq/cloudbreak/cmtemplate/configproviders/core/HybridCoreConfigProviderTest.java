package com.sequenceiq.cloudbreak.cmtemplate.configproviders.core;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles.HDFS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles.NAMENODE;
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
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsConfigHelper;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.TrustView;
import com.sequenceiq.cloudbreak.sdx.RdcView;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.DatalakeView;

class HybridCoreConfigProviderTest {

    @Mock
    private HdfsConfigHelper hdfsConfigHelper;

    @InjectMocks
    private HybridCoreConfigProvider underTest;

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Mock
    private TemplatePreparationObject source;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        when(cmTemplateProcessor.isRoleTypePresentInService(HDFS, Lists.newArrayList(NAMENODE))).thenReturn(true);
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

        assertEquals(1, configs.size());
        assertEquals("hadoop_rpc_protection", configs.get(0).getName());
        assertEquals("privacy", configs.get(0).getValue());
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
    void isConfigurationNeededDelegatesToCmTemplateProcessor() {
        when(cmTemplateProcessor.isHybridDatahub(source)).thenReturn(true);
        assertTrue(underTest.isConfigurationNeeded(cmTemplateProcessor, source));
        when(cmTemplateProcessor.isHybridDatahub(source)).thenReturn(false);
        assertFalse(underTest.isConfigurationNeeded(cmTemplateProcessor, source));
    }
}
