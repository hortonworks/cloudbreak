package com.sequenceiq.cloudbreak.cmtemplate;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static java.util.Optional.ofNullable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase.HbaseVolumeConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsVolumeConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn.YarnVolumeConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.zookeeper.ZooKeeperVolumeConfigProvider;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
public class CmHostGroupRoleConfigProviderProcessorTest {

    @InjectMocks
    private final CmHostGroupRoleConfigProviderProcessor underTest = new CmHostGroupRoleConfigProviderProcessor();

    @Spy
    private final List<CmHostGroupRoleConfigProvider> configProviders = new ArrayList<>(List.of(
            new HdfsVolumeConfigProvider(), new YarnVolumeConfigProvider(), new ZooKeeperVolumeConfigProvider(), new HbaseVolumeConfigProvider()
    ));

    private TemplatePreparationObject templatePreparator;

    private CmTemplateProcessor templateProcessor;

    @Test
    public void testGetRoleConfigsWithSingleRolesPerHostGroup() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        setup("input/clouderamanager.bp", Builder.builder().withHostgroupViews(Set.of(master, worker)));

        underTest.process(templateProcessor, templatePreparator);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = mapRoleConfigs();
        assertEquals(List.of(config("dfs_data_dir_list", "/hadoopfs/fs1/datanode,/hadoopfs/fs2/datanode")), roleConfigs.get("hdfs-DATANODE-BASE"));
        assertEquals(List.of(config("dfs_name_dir_list", "/hadoopfs/fs1/namenode")), roleConfigs.get("hdfs-NAMENODE-BASE"));
        assertEquals(List.of(config("fs_checkpoint_dir_list", "/hadoopfs/fs1/namesecondary")), roleConfigs.get("hdfs-SECONDARYNAMENODE-BASE"));
        assertEquals(
                List.of(
                        config("yarn_nodemanager_local_dirs", "/hadoopfs/fs1/nodemanager,/hadoopfs/fs2/nodemanager"),
                        config("yarn_nodemanager_log_dirs", "/hadoopfs/fs1/nodemanager/log,/hadoopfs/fs2/nodemanager/log")
                ),
                roleConfigs.get("yarn-NODEMANAGER-BASE")
        );
    }

    @Test
    public void testGetRoleConfigsWithSingleRolesPerHostGroupWithCustomRefNames() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        setup("input/clouderamanager-custom-ref.bp", Builder.builder().withHostgroupViews(Set.of(master, worker)));

        underTest.process(templateProcessor, templatePreparator);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = mapRoleConfigs();
        assertEquals(List.of(config("dfs_data_dir_list", "/hadoopfs/fs1/datanode,/hadoopfs/fs2/datanode")), roleConfigs.get("dn"));
        assertEquals(List.of(config("dfs_name_dir_list", "/hadoopfs/fs1/namenode")), roleConfigs.get("nn"));
    }

    @Test
    public void testGetRoleConfigsWithOneHostGroupZeroAttachedVolumes() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 2);
        setup("input/clouderamanager.bp", Builder.builder().withHostgroupViews(Set.of(master, worker)));

        underTest.process(templateProcessor, templatePreparator);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = mapRoleConfigs();
        assertEquals(List.of(config("dfs_name_dir_list", "/hadoopfs/fs1/namenode")), roleConfigs.get("hdfs-NAMENODE-BASE"));
        assertEquals(List.of(config("fs_checkpoint_dir_list", "/hadoopfs/fs1/namesecondary")), roleConfigs.get("hdfs-SECONDARYNAMENODE-BASE"));
        assertEquals(List.of(config("dfs_data_dir_list", "/hadoopfs/root1/datanode")), roleConfigs.get("hdfs-DATANODE-BASE"));
    }

    @Test
    public void testGetRoleConfigsWithAllHostGroupZeroAttachedVolumes() {
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 2);
        setup("input/clouderamanager.bp", Builder.builder().withHostgroupViews(Set.of(master, worker)));

        underTest.process(templateProcessor, templatePreparator);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = mapRoleConfigs();
        List<ApiClusterTemplateConfig> masterNN = roleConfigs.get("hdfs-NAMENODE-BASE");
        List<ApiClusterTemplateConfig> masterSN = roleConfigs.get("hdfs-SECONDARYNAMENODE-BASE");
        List<ApiClusterTemplateConfig> workerDN = roleConfigs.get("hdfs-DATANODE-BASE");
        List<ApiClusterTemplateConfig> workerNM = roleConfigs.get("yarn-NODEMANAGER-BASE");

        assertEquals(1, masterNN.size());
        assertEquals(1, masterSN.size());
        assertEquals(1, workerDN.size());
        assertEquals("dfs_name_dir_list", masterNN.get(0).getName());
        assertEquals("/hadoopfs/root1/namenode", masterNN.get(0).getValue());
        assertEquals("fs_checkpoint_dir_list", masterSN.get(0).getName());
        assertEquals("/hadoopfs/root1/namesecondary", masterSN.get(0).getValue());
        assertEquals("dfs_data_dir_list", workerDN.get(0).getName());
        assertEquals("/hadoopfs/root1/datanode", workerDN.get(0).getValue());
        assertEquals("yarn_nodemanager_local_dirs", workerNM.get(0).getName());
        assertEquals("/hadoopfs/root1/nodemanager", workerNM.get(0).getValue());
        assertEquals("yarn_nodemanager_log_dirs", workerNM.get(1).getName());
        assertEquals("/hadoopfs/root1/nodemanager/log", workerNM.get(1).getValue());
    }

    @Test
    public void clonesBaseRoleConfigForHostGroups() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        HostgroupView compute = new HostgroupView("compute", 3, InstanceGroupType.CORE, 2);
        setup("input/clouderamanager-3hg-same-DN-role.bp", Builder.builder().withHostgroupViews(Set.of(master, worker, compute)));

        underTest.process(templateProcessor, templatePreparator);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = mapRoleConfigs();
        assertNull(roleConfigs.get("hdfs-DATANODE-BASE"));
        assertEquals(List.of(config("dfs_data_dir_list", "/hadoopfs/fs1/datanode,/hadoopfs/fs2/datanode")),
                roleConfigs.get("hdfs-DATANODE-worker"));
        assertEquals(List.of(config("dfs_data_dir_list", "/hadoopfs/fs1/datanode,/hadoopfs/fs2/datanode,/hadoopfs/fs3/datanode")),
                roleConfigs.get("hdfs-DATANODE-compute"));
    }

    @Test
    public void retainsExistingConfigsInEachClone() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        HostgroupView compute = new HostgroupView("compute", 3, InstanceGroupType.CORE, 2);
        setup("input/clouderamanager-3hg-same-DN-role.bp", Builder.builder().withHostgroupViews(Set.of(master, worker, compute)));

        ApiClusterTemplateConfig existingConfig = config("existing_config", "some_Value");

        templateProcessor.getServiceByType("HDFS").ifPresent(
                service -> service.getRoleConfigGroups().stream()
                        .filter(rcg -> "hdfs-DATANODE-BASE".equals(rcg.getRefName()))
                        .forEach(rcg -> rcg.addConfigsItem(existingConfig))
        );

        underTest.process(templateProcessor, templatePreparator);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = mapRoleConfigs();
        assertNull(roleConfigs.get("hdfs-DATANODE-BASE"));
        assertEquals(Set.of(existingConfig, config("dfs_data_dir_list", "/hadoopfs/fs1/datanode,/hadoopfs/fs2/datanode")),
                Set.copyOf(roleConfigs.get("hdfs-DATANODE-worker")));
        assertEquals(Set.of(existingConfig, config("dfs_data_dir_list", "/hadoopfs/fs1/datanode,/hadoopfs/fs2/datanode,/hadoopfs/fs3/datanode")),
                Set.copyOf(roleConfigs.get("hdfs-DATANODE-compute")));
    }

    @Test
    public void testGetRoleConfigsWithDifferentDataNodeRoleForMultipleGroups() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        HostgroupView compute = new HostgroupView("compute", 3, InstanceGroupType.CORE, 2);
        setup("input/clouderamanager-3hg-different-DN-role.bp", Builder.builder().withHostgroupViews(Set.of(master, worker, compute)));

        underTest.process(templateProcessor, templatePreparator);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = mapRoleConfigs();

        assertEquals(
                List.of(config("dfs_name_dir_list", "/hadoopfs/fs1/namenode")),
                roleConfigs.get("hdfs-NAMENODE-BASE"));
        assertEquals(
                List.of(config("dfs_data_dir_list", "/hadoopfs/fs1/datanode,/hadoopfs/fs2/datanode")),
                roleConfigs.get("hdfs-DATANODE-BASE"));
        assertEquals(
                List.of(config("dfs_data_dir_list", "/hadoopfs/fs1/datanode,/hadoopfs/fs2/datanode,/hadoopfs/fs3/datanode")),
                roleConfigs.get("hdfs-DATANODE-compute"));

    }

    @Test
    public void testGetRoleConfigsWithDifferentDataNodeRoleForMultipleGroupsNoComputeDisks() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        HostgroupView compute = new HostgroupView("compute", 0, InstanceGroupType.CORE, 2);
        setup("input/clouderamanager-3hg-different-DN-role.bp", Builder.builder().withHostgroupViews(Set.of(master, worker, compute)));

        underTest.process(templateProcessor, templatePreparator);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = mapRoleConfigs();
        List<ApiClusterTemplateConfig> masterNN = roleConfigs.get("hdfs-NAMENODE-BASE");
        List<ApiClusterTemplateConfig> workerDN = roleConfigs.get("hdfs-DATANODE-BASE");
        List<ApiClusterTemplateConfig> computeDN = roleConfigs.get("hdfs-DATANODE-compute");

        assertEquals(1, workerDN.size());
        assertEquals("dfs_data_dir_list", workerDN.get(0).getName());
        assertEquals("/hadoopfs/fs1/datanode,/hadoopfs/fs2/datanode", workerDN.get(0).getValue());
        assertEquals(1, masterNN.size());
        assertEquals("dfs_name_dir_list", masterNN.get(0).getName());
        assertEquals(1, computeDN.size());
        assertEquals("dfs_data_dir_list", computeDN.get(0).getName());
        assertEquals("/hadoopfs/root1/datanode", computeDN.get(0).getValue());
    }

    @Test
    public void testGenerateConfigsWhenHdfsJournalNodeNull() {
        HostgroupView master1 = new HostgroupView("master1", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView master2 = new HostgroupView("master2", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        HostgroupView compute = new HostgroupView("compute", 3, InstanceGroupType.CORE, 2);
        HostgroupView quorum = new HostgroupView("quorum", 3, InstanceGroupType.CORE, 2);
        setup("input/cb5660.bp", Builder.builder().withHostgroupViews(Set.of(master1, master2, worker, compute, quorum)));

        Map<String, Map<String, List<ApiClusterTemplateConfig>>> actual = underTest.generateConfigs(templateProcessor, templatePreparator);
        assertNotNull(actual.get("hdfs-JOURNALNODE-BASE"));
        assertNotNull(actual.get("zookeeper-SERVER-BASE"));
    }

    @Test
    public void testProcessWhenSharedConfigThenAddVolumeConfig() {
        HostgroupView master1 = new HostgroupView("master1", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView master2 = new HostgroupView("master2", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        HostgroupView compute = new HostgroupView("compute", 3, InstanceGroupType.CORE, 2);
        HostgroupView quorum = new HostgroupView("quorum", 3, InstanceGroupType.CORE, 2);
        setup("input/cb5660.bp", Builder.builder().withHostgroupViews(Set.of(master1, master2, worker, compute, quorum)));
        underTest.process(templateProcessor, templatePreparator);
        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = mapRoleConfigs();
        List<ApiClusterTemplateConfig> journalNodeBase = roleConfigs.get("hdfs-JOURNALNODE-BASE");
        assertEquals(1, journalNodeBase.size());
        assertEquals("dfs_journalnode_edits_dir", journalNodeBase.get(0).getName());
        assertEquals("/hadoopfs/fs1/journalnode", journalNodeBase.get(0).getValue());
        List<ApiClusterTemplateConfig> zkServerBase = roleConfigs.get("zookeeper-SERVER-BASE");
        assertEquals("dataDir", zkServerBase.get(0).getName());
        assertEquals("/hadoopfs/fs1/zookeeper", zkServerBase.get(0).getValue());
        assertEquals("dataLogDir", zkServerBase.get(1).getName());
        assertEquals("/hadoopfs/fs1/zookeeper", zkServerBase.get(1).getValue());
    }

    @Test
    public void testGetRoleConfigsWithEphemeralVolumes() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, Collections.<String>emptySet(),
                Sets.newHashSet(new VolumeTemplate()), TemporaryStorage.EPHEMERAL_VOLUMES, 1, 100);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, Collections.<String>emptySet(),
                Sets.newHashSet(new VolumeTemplate()), TemporaryStorage.EPHEMERAL_VOLUMES, 3, 300);
        setup("input/clouderamanager.bp", Builder.builder().withHostgroupViews(Set.of(master, worker)));

        underTest.process(templateProcessor, templatePreparator);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = mapRoleConfigs();
        assertEquals(
                List.of(
                        config("hbase_bucketcache_ioengine", "file:/hadoopfs/ephfs1/hbase_cache")
                ),
                roleConfigs.get("hbase-REGIONSERVER-BASE"));
        assertEquals(
                List.of(
                        config("yarn_nodemanager_local_dirs", "/hadoopfs/ephfs1/nodemanager,/hadoopfs/ephfs2/nodemanager,/hadoopfs/ephfs3/nodemanager"),
                        config("yarn_nodemanager_log_dirs", "/hadoopfs/ephfs1/nodemanager/log,/hadoopfs/ephfs2/nodemanager/log," +
                                "/hadoopfs/ephfs3/nodemanager/log")
                ),
                roleConfigs.get("yarn-NODEMANAGER-BASE")
        );
    }

    @Test
    public void testGetRoleConfigsWithAttachedVolumes() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, Collections.<String>emptySet(),
                Sets.newHashSet(new VolumeTemplate()), TemporaryStorage.ATTACHED_VOLUMES, 0, 0);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, Collections.<String>emptySet(),
                Sets.newHashSet(new VolumeTemplate()), TemporaryStorage.ATTACHED_VOLUMES, 0, 0);
        setup("input/clouderamanager.bp", Builder.builder().withHostgroupViews(Set.of(master, worker)));

        underTest.process(templateProcessor, templatePreparator);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = mapRoleConfigs();
        assertEquals(
                List.of(
                        config("hbase_bucketcache_ioengine", "offheap")
                ),
                roleConfigs.get("hbase-REGIONSERVER-BASE"));
        assertEquals(
                List.of(
                        config("yarn_nodemanager_local_dirs", "/hadoopfs/fs1/nodemanager,/hadoopfs/fs2/nodemanager"),
                        config("yarn_nodemanager_log_dirs", "/hadoopfs/fs1/nodemanager/log,/hadoopfs/fs2/nodemanager/log")
                ),
                roleConfigs.get("yarn-NODEMANAGER-BASE")
        );
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }

    private Map<String, List<ApiClusterTemplateConfig>> mapRoleConfigs() {
        return templateProcessor.getTemplate().getServices().stream()
                .filter(service -> Objects.nonNull(service.getRoleConfigGroups()))
                .flatMap(service -> service.getRoleConfigGroups().stream())
                .collect(Collectors.toMap(ApiClusterTemplateRoleConfigGroup::getRefName, rcg -> ofNullable(rcg.getConfigs()).orElseGet(List::of)));
    }

    private void setup(String path, TemplatePreparationObject.Builder builder) {
        String blueprintText = getBlueprintText(path);
        templateProcessor = new CmTemplateProcessor(blueprintText);
        templatePreparator = builder
                .withBlueprintView(new BlueprintView(blueprintText, "CDP", "1.0", null, templateProcessor))
                .withGeneralClusterConfigs(new GeneralClusterConfigs())
                .build();
        templateProcessor.addInstantiator(null, templatePreparator, "sdx");
    }

}