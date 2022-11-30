package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
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
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;

class HdfsRoleConfigProviderTest {

    private static final Set<String> NN_HA_PROPERTIES =
            Set.of("autofailover_enabled", "dfs_federation_namenode_nameservice", "dfs_namenode_quorum_journal_name");

    private static final String TEST_USER_CRN = "crn:cdp:iam:us-west-1:accid:user:mockuser@cloudera.com";

    private EntitlementService entitlementService = mock(EntitlementService.class);

    private final HdfsRoleConfigProvider subject = new HdfsRoleConfigProvider(entitlementService);

    @Test
    void nameNodeHA() {
        when(entitlementService.isSDXOptimizedConfigurationEnabled(anyString())).thenReturn(false);
        HostgroupView gateway = new HostgroupView("gateway", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.CORE, 2);
        HostgroupView quorum = new HostgroupView("quorum", 0, InstanceGroupType.CORE, 3);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 3);
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/namenode-ha.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withHostgroupViews(Set.of(gateway, master, quorum, worker))
                .withBlueprintView(new BlueprintView(inputJson, "CDP", "1.0", cmTemplateProcessor))
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
        when(entitlementService.isSDXOptimizedConfigurationEnabled(anyString())).thenReturn(false);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.CORE, 1);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 3);
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withHostgroupViews(Set.of(master, worker))
                .withBlueprintView(new BlueprintView(inputJson, "CDP", "1.0", cmTemplateProcessor))
                .build();

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            Map<String, List<ApiClusterTemplateConfig>> roleConfigs = subject.getRoleConfigs(cmTemplateProcessor, preparationObject);

            assertEquals(List.of(), roleConfigs.get("hdfs-NAMENODE-BASE"));
            assertEquals(List.of(config("dfs_datanode_failed_volumes_tolerated", "0")), roleConfigs.get("hdfs-DATANODE-BASE"));
        });
    }

    @Test
    void optimizedHDFSReplicaHA() {
        when(entitlementService.isSDXOptimizedConfigurationEnabled(anyString())).thenReturn(true);
        HostgroupView gateway = new HostgroupView("gateway", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.CORE, 2);
        HostgroupView quorum = new HostgroupView("quorum", 0, InstanceGroupType.CORE, 3);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 3);
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/namenode-ha.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withStackType(StackType.DATALAKE)
                .withHostgroupViews(Set.of(gateway, master, quorum, worker))
                .withBlueprintView(new BlueprintView(inputJson, "CDP", "1.0", cmTemplateProcessor))
                .build();

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {

            Map<String, List<ApiClusterTemplateConfig>> roleConfigs = subject.getRoleConfigs(cmTemplateProcessor, preparationObject);

            List<ApiClusterTemplateConfig> namenodeConfigs = roleConfigs.get("hdfs-NAMENODE-BASE");
            Map<String, ApiClusterTemplateConfig> configMap = cmTemplateProcessor.mapByName(namenodeConfigs);
            assertEquals(NN_HA_PROPERTIES, configMap.keySet());
            assertEquals("true", configMap.get("autofailover_enabled").getValue());
            assertEquals(List.of(
                            config("dfs_datanode_failed_volumes_tolerated", "0"),
                            config("dfs_replication", "2"),
                            config("dfs_encrypt_data_transfer", "true")),
                    roleConfigs.get("hdfs-DATANODE-BASE"));

        });
    }

    @Test
    void optimizedHDFSDataEncryptHA() {
        when(entitlementService.isSDXOptimizedConfigurationEnabled(anyString())).thenReturn(true);
        HostgroupView gateway = new HostgroupView("gateway", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.CORE, 2);
        HostgroupView quorum = new HostgroupView("quorum", 0, InstanceGroupType.CORE, 3);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 3);
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/namenode-ha.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withStackType(StackType.DATALAKE)
                .withHostgroupViews(Set.of(gateway, master, quorum, worker))
                .withBlueprintView(new BlueprintView(inputJson, "CDP", "1.0", cmTemplateProcessor))
                .build();

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {

            Map<String, List<ApiClusterTemplateConfig>> roleConfigs = subject.getRoleConfigs(cmTemplateProcessor, preparationObject);

            List<ApiClusterTemplateConfig> namenodeConfigs = roleConfigs.get("hdfs-NAMENODE-BASE");
            Map<String, ApiClusterTemplateConfig> configMap = cmTemplateProcessor.mapByName(namenodeConfigs);
            assertEquals(NN_HA_PROPERTIES, configMap.keySet());
            assertEquals("true", configMap.get("autofailover_enabled").getValue());
            assertEquals(List.of(
                            config("dfs_datanode_failed_volumes_tolerated", "0"),
                            config("dfs_replication", "2"),
                            config("dfs_encrypt_data_transfer", "true")),
                    roleConfigs.get("hdfs-DATANODE-BASE"));

        });
    }

    @Test
    void optimizedHDFSDataEncryptHAEtitlementDisabled() {
        when(entitlementService.isSDXOptimizedConfigurationEnabled(anyString())).thenReturn(false);
        HostgroupView gateway = new HostgroupView("gateway", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.CORE, 2);
        HostgroupView quorum = new HostgroupView("quorum", 0, InstanceGroupType.CORE, 3);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 3);
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/namenode-ha.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withStackType(StackType.DATALAKE)
                .withHostgroupViews(Set.of(gateway, master, quorum, worker))
                .withBlueprintView(new BlueprintView(inputJson, "CDP", "1.0", cmTemplateProcessor))
                .build();

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {

            Map<String, List<ApiClusterTemplateConfig>> roleConfigs = subject.getRoleConfigs(cmTemplateProcessor, preparationObject);

            List<ApiClusterTemplateConfig> namenodeConfigs = roleConfigs.get("hdfs-NAMENODE-BASE");
            Map<String, ApiClusterTemplateConfig> configMap = cmTemplateProcessor.mapByName(namenodeConfigs);
            assertEquals(NN_HA_PROPERTIES, configMap.keySet());
            assertEquals("true", configMap.get("autofailover_enabled").getValue());
            assertEquals(List.of(config("dfs_datanode_failed_volumes_tolerated", "0")), roleConfigs.get("hdfs-DATANODE-BASE"));
        });
    }

}
