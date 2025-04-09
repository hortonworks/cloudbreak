package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.getSafetyValveProperty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;

class HdfsRoleConfigProviderTest {

    private static final Set<String> NN_HA_PROPERTIES =
            Set.of("autofailover_enabled", "dfs_federation_namenode_nameservice", "dfs_namenode_quorum_journal_name");

    private static final String TEST_USER_CRN = "crn:cdp:iam:us-west-1:accid:user:mockuser@cloudera.com";

    private final EntitlementService entitlementService = mock(EntitlementService.class);

    private final HdfsRoleConfigProvider subject = new HdfsRoleConfigProvider(entitlementService);

    @Test
    void nameNodeHA() {
        when(entitlementService.isWireEncryptionEnabled(anyString())).thenReturn(false);
        HostgroupView gateway = new HostgroupView("gateway", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.CORE, 2);
        HostgroupView quorum = new HostgroupView("quorum", 0, InstanceGroupType.CORE, 3);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 3);
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/namenode-ha.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withHostgroupViews(Set.of(gateway, master, quorum, worker))
                .withBlueprintView(new BlueprintView(inputJson, "CDP", "1.0", null, cmTemplateProcessor))
                .build();

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {

            Map<String, List<ApiClusterTemplateConfig>> roleConfigs = subject.getRoleConfigs(cmTemplateProcessor, preparationObject);

            List<ApiClusterTemplateConfig> namenodeConfigs = roleConfigs.get("hdfs-NAMENODE-BASE");
            Map<String, ApiClusterTemplateConfig> configMap = cmTemplateProcessor.mapByName(namenodeConfigs);
            assertEquals(NN_HA_PROPERTIES, configMap.keySet());
            assertEquals("true", configMap.get("autofailover_enabled").getValue());
        });
    }

    @Test
    void nonHA() {
        when(entitlementService.isWireEncryptionEnabled(anyString())).thenReturn(false);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.CORE, 1);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 3);
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withHostgroupViews(Set.of(master, worker))
                .withBlueprintView(new BlueprintView(inputJson, "CDP", "1.0", null, cmTemplateProcessor))
                .build();

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            Map<String, List<ApiClusterTemplateConfig>> roleConfigs = subject.getRoleConfigs(cmTemplateProcessor, preparationObject);

            assertEquals(List.of(
                    config("dfs_datanode_failed_volumes_tolerated", "0")),
                    roleConfigs.get("hdfs-DATANODE-BASE"));
        });
    }

    @Test
    void optimizedHDFSReplicaHA() {
        when(entitlementService.isWireEncryptionEnabled(anyString())).thenReturn(true);
        HostgroupView gateway = new HostgroupView("gateway", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.CORE, 2);
        HostgroupView quorum = new HostgroupView("quorum", 0, InstanceGroupType.CORE, 3);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 3);
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/namenode-ha.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withStackType(StackType.DATALAKE)
                .withHostgroupViews(Set.of(gateway, master, quorum, worker))
                .withBlueprintView(new BlueprintView(inputJson, "CDP", "1.0", null, cmTemplateProcessor))
                .build();

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {

            Map<String, List<ApiClusterTemplateConfig>> roleConfigs = subject.getRoleConfigs(cmTemplateProcessor, preparationObject);

            List<ApiClusterTemplateConfig> namenodeConfigs = roleConfigs.get("hdfs-NAMENODE-BASE");
            Map<String, ApiClusterTemplateConfig> configMap = cmTemplateProcessor.mapByName(namenodeConfigs);
            StringBuilder safetyValveValue = new StringBuilder();
            safetyValveValue.append(getSafetyValveProperty("ipc.client.connect.max.retries.on.timeouts", "5"));
            safetyValveValue.append(getSafetyValveProperty("ipc.client.connect.timeout", "5000"));
            assertEquals(NN_HA_PROPERTIES, configMap.keySet());
            assertEquals("true", configMap.get("autofailover_enabled").getValue());
            assertEquals(List.of(
                            config("dfs_datanode_failed_volumes_tolerated", "0"),
                            config("dfs_encrypt_data_transfer", "true"),
                            config("datanode_config_safety_valve", safetyValveValue.toString())),
                    roleConfigs.get("hdfs-DATANODE-BASE"));

        });
    }

    @Test
    void optimizedHDFSReplicaHAServiceConfig() {
        when(entitlementService.isWireEncryptionEnabled(anyString())).thenReturn(true);
        HostgroupView gateway = new HostgroupView("gateway", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.CORE, 2);
        HostgroupView quorum = new HostgroupView("quorum", 0, InstanceGroupType.CORE, 3);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 3);
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/namenode-ha.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withStackType(StackType.DATALAKE)
                .withHostgroupViews(Set.of(gateway, master, quorum, worker))
                .withBlueprintView(new BlueprintView(inputJson, "CDP", "1.0", null, cmTemplateProcessor))
                .build();

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {

            List<ApiClusterTemplateConfig> serviceConfigs = subject.getServiceConfigs(cmTemplateProcessor, preparationObject);

            Map<String, ApiClusterTemplateConfig> configMap = cmTemplateProcessor.mapByName(serviceConfigs);
            assertEquals(3, serviceConfigs.size());
            assertEquals("true", configMap.get("dfs_encrypt_data_transfer").getValue());
            assertEquals("privacy", configMap.get("dfs_data_transfer_protection").getValue());
            assertEquals("privacy", configMap.get("hadoop_rpc_protection").getValue());
        });
    }

    @Test
    void optimizedHDFSReplicaHAServiceConfigWhenGovCloudShouldReturnRpcProtectionTrue() {
        when(entitlementService.isWireEncryptionEnabled(anyString())).thenReturn(false);
        HostgroupView gateway = new HostgroupView("gateway", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.CORE, 2);
        HostgroupView quorum = new HostgroupView("quorum", 0, InstanceGroupType.CORE, 3);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 3);
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setGovCloud(true);
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/namenode-ha.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withStackType(StackType.DATALAKE)
                .withHostgroupViews(Set.of(gateway, master, quorum, worker))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withBlueprintView(new BlueprintView(inputJson, "CDP", "1.0", null, cmTemplateProcessor))
                .build();

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            List<ApiClusterTemplateConfig> serviceConfigs = subject.getServiceConfigs(cmTemplateProcessor, preparationObject);
            Map<String, ApiClusterTemplateConfig> configMap = cmTemplateProcessor.mapByName(serviceConfigs);
            assertEquals(1, serviceConfigs.size());
            assertEquals("privacy", configMap.get("hadoop_rpc_protection").getValue());
        });
    }

    @Test
    void optimizedHDFSDataEncryptHA() {
        when(entitlementService.isWireEncryptionEnabled(anyString())).thenReturn(true);
        HostgroupView gateway = new HostgroupView("gateway", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.CORE, 2);
        HostgroupView quorum = new HostgroupView("quorum", 0, InstanceGroupType.CORE, 3);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 3);
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/namenode-ha.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withStackType(StackType.DATALAKE)
                .withHostgroupViews(Set.of(gateway, master, quorum, worker))
                .withBlueprintView(new BlueprintView(inputJson, "CDP", "1.0", null, cmTemplateProcessor))
                .build();

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {

            Map<String, List<ApiClusterTemplateConfig>> roleConfigs = subject.getRoleConfigs(cmTemplateProcessor, preparationObject);

            List<ApiClusterTemplateConfig> namenodeConfigs = roleConfigs.get("hdfs-NAMENODE-BASE");
            Map<String, ApiClusterTemplateConfig> configMap = cmTemplateProcessor.mapByName(namenodeConfigs);
            assertEquals(NN_HA_PROPERTIES, configMap.keySet());
            assertEquals("true", configMap.get("autofailover_enabled").getValue());
            StringBuilder safetyValveValue = new StringBuilder();
            safetyValveValue.append(getSafetyValveProperty("ipc.client.connect.max.retries.on.timeouts", "5"));
            safetyValveValue.append(getSafetyValveProperty("ipc.client.connect.timeout", "5000"));
            assertEquals(List.of(
                            config("dfs_datanode_failed_volumes_tolerated", "0"),
                            config("dfs_encrypt_data_transfer", "true"),
                            config("datanode_config_safety_valve", safetyValveValue.toString())),
                    roleConfigs.get("hdfs-DATANODE-BASE"));

        });
    }

    @Test
    void optimizedHDFSDataEncryptHAEtitlementDisabled() {
        when(entitlementService.isWireEncryptionEnabled(anyString())).thenReturn(false);
        HostgroupView gateway = new HostgroupView("gateway", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.CORE, 2);
        HostgroupView quorum = new HostgroupView("quorum", 0, InstanceGroupType.CORE, 3);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 3);
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/namenode-ha.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withStackType(StackType.DATALAKE)
                .withHostgroupViews(Set.of(gateway, master, quorum, worker))
                .withBlueprintView(new BlueprintView(inputJson, "CDP", "1.0", null, cmTemplateProcessor))
                .build();

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {

            Map<String, List<ApiClusterTemplateConfig>> roleConfigs = subject.getRoleConfigs(cmTemplateProcessor, preparationObject);

            StringBuilder safetyValveValue = new StringBuilder();
            safetyValveValue.append(getSafetyValveProperty("ipc.client.connect.max.retries.on.timeouts", "5"));
            safetyValveValue.append(getSafetyValveProperty("ipc.client.connect.timeout", "5000"));
            List<ApiClusterTemplateConfig> namenodeConfigs = roleConfigs.get("hdfs-NAMENODE-BASE");
            Map<String, ApiClusterTemplateConfig> configMap = cmTemplateProcessor.mapByName(namenodeConfigs);
            assertEquals(NN_HA_PROPERTIES, configMap.keySet());
            assertEquals("true", configMap.get("autofailover_enabled").getValue());
            assertEquals(List.of(
                    config("dfs_datanode_failed_volumes_tolerated", "0"),
                    config("datanode_config_safety_valve", safetyValveValue.toString())),
                    roleConfigs.get("hdfs-DATANODE-BASE"));
        });
    }

    @Test
    void datalakeShouldHaveIpcClientProperties() {
        HostgroupView gateway = new HostgroupView("gateway", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.CORE, 2);
        HostgroupView quorum = new HostgroupView("quorum", 0, InstanceGroupType.CORE, 3);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 3);
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/namenode-ha.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withStackType(StackType.DATALAKE)
                .withHostgroupViews(Set.of(gateway, master, quorum, worker))
                .withBlueprintView(new BlueprintView(inputJson, "CDP", "1.0", null, cmTemplateProcessor))
                .build();

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            Map<String, List<ApiClusterTemplateConfig>> roleConfigs = subject.getRoleConfigs(cmTemplateProcessor, preparationObject);

            StringBuilder safetyValveValue = new StringBuilder();
            safetyValveValue.append(getSafetyValveProperty("ipc.client.connect.max.retries.on.timeouts", "5"));
            safetyValveValue.append(getSafetyValveProperty("ipc.client.connect.timeout", "5000"));
            List<ApiClusterTemplateConfig> namenodeConfigs = roleConfigs.get("hdfs-NAMENODE-BASE");
            Map<String, ApiClusterTemplateConfig> configMap = cmTemplateProcessor.mapByName(namenodeConfigs);

            assertEquals(List.of(
                            config("dfs_datanode_failed_volumes_tolerated", "0"),
                            config("datanode_config_safety_valve", safetyValveValue.toString())),
                    roleConfigs.get("hdfs-DATANODE-BASE"));
        });
    }

    @Test
    void dataNodeFromDatahubDoesntContainDatanodeConfigSafetyValve() {
        HostgroupView gateway = new HostgroupView("gateway", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.CORE, 2);
        HostgroupView quorum = new HostgroupView("quorum", 0, InstanceGroupType.CORE, 3);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 3);
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/namenode-ha.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withStackType(StackType.WORKLOAD)
                .withHostgroupViews(Set.of(gateway, master, quorum, worker))
                .withBlueprintView(new BlueprintView(inputJson, "CDP", "1.0", null, cmTemplateProcessor))
                .build();

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            Map<String, List<ApiClusterTemplateConfig>> roleConfigs = subject.getRoleConfigs(cmTemplateProcessor, preparationObject);
            List<ApiClusterTemplateConfig> namenodeConfigs = roleConfigs.get("hdfs-NAMENODE-BASE");
            Map<String, ApiClusterTemplateConfig> configMap = cmTemplateProcessor.mapByName(namenodeConfigs);

            assertThat(configMap).doesNotContainKey("datanode_config_safety_valve");
        });
    }
}
