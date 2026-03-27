package com.sequenceiq.cloudbreak.cmtemplate.configproviders.core;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.getSafetyValveProperty;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.CORE_DEFAULTFS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.CORE_SITE_SAFETY_VALVE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles.HDFS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles.NAMENODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.views.DatalakeView;
import com.sequenceiq.common.model.CloudStorageCdpService;

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

    @Test
    void stubDfsProperties() {
        when(cmTemplateProcessor.isRoleTypePresentInService(HDFS, Lists.newArrayList(NAMENODE))).thenReturn(false);
        BaseFileSystemConfigurationsView fileSystemConfigurationsView = mock();
        StorageLocationView storageLocationView = mock();
        when(storageLocationView.getProperty()).thenReturn(CloudStorageCdpService.REMOTE_FS.name());
        when(storageLocationView.getValue()).thenReturn("hdfs://ns1");
        when(fileSystemConfigurationsView.getLocations()).thenReturn(List.of(storageLocationView));
        when(source.getFileSystemConfigurationView()).thenReturn(Optional.of(fileSystemConfigurationsView));

        TrustView trustView = mock(TrustView.class);
        when(source.getTrustView()).thenReturn(Optional.of(trustView));
        when(trustView.realm()).thenReturn("realmX");

        when(source.getDatalakeView()).thenReturn(Optional.of(mock()));

        when(hdfsConfigHelper.getNameService(any())).thenReturn("ns1");
        when(hdfsConfigHelper.getNameServiceConfigSafetyValveValue(any())).thenReturn("<NameServiceConfigSafetyValveValue>");

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(cmTemplateProcessor, source);

        assertThat(result)
                .contains(config(CORE_DEFAULTFS, "hdfs://ns1"))
                .anyMatch(config ->
                        config.getName().equals(CORE_SITE_SAFETY_VALVE)
                                && config.getValue().contains("<NameServiceConfigSafetyValveValue>")
                                && config.getValue().contains(getSafetyValveProperty("dfs.namenode.kerberos.principal", "hdfs/_HOST@REALMX"))
                                && config.getValue().contains(getSafetyValveProperty("dfs.nameservices", "ns1"))
                );
    }
}
