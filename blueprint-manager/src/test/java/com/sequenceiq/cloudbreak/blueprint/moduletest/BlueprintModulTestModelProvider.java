package com.sequenceiq.cloudbreak.blueprint.moduletest;

import static com.sequenceiq.cloudbreak.TestUtil.gatewayEnabled;
import static com.sequenceiq.cloudbreak.TestUtil.hostGroup;
import static com.sequenceiq.cloudbreak.TestUtil.kerberosConfig;
import static com.sequenceiq.cloudbreak.TestUtil.ldapConfig;
import static com.sequenceiq.cloudbreak.TestUtil.rdsConfig;
import static com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType.CORE;
import static com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType.GATEWAY;
import static com.sequenceiq.cloudbreak.blueprint.filesystem.BlueprintTestUtil.generalBlueprintView;
import static com.sequenceiq.cloudbreak.blueprint.filesystem.BlueprintTestUtil.generalClusterConfigs;
import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModulTest.BLUEPRINT_UPDATER_TEST_INPUTS;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.model.ExecutorType;
import com.sequenceiq.cloudbreak.api.model.filesystem.S3FileSystem;
import com.sequenceiq.cloudbreak.api.model.rds.RdsType;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject.Builder;
import com.sequenceiq.cloudbreak.blueprint.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.blueprint.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.blueprint.nifi.HdfConfigs;
import com.sequenceiq.cloudbreak.blueprint.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.blueprint.template.views.SharedServiceConfigsView;
import com.sequenceiq.cloudbreak.blueprint.templates.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.blueprint.testrepeater.TestFile;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

class BlueprintModulTestModelProvider {

    private BlueprintModulTestModelProvider() {
    }

    static BlueprintPreparationObject blueprintObjectWhenHiveAndRdsPresentedThenRdsHiveMetastoreShouldConfigured() {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(RdsType.HIVE))))
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenRangerAndRdsPresentedThenRdsRangerShouldConfigured() {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(RdsType.RANGER))))
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenDruidAndRdsPresentedThenRdsDruidShouldConfigured() {
        return getPreparedBuilder()
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(RdsType.DRUID))))
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenSuperSetAndRdsPresentedThenRdsDruidShouldConfigured() {
        return getPreparedBuilder()
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(RdsType.SUPERSET))))
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenDruidSuperSetAndRdsPresentedThenRdsDruidShouldConfigured() {
        return getPreparedBuilder()
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(RdsType.SUPERSET))))
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenLdapPresentedThenRangerAndHadoopLdapShouldConfigured() {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withLdapConfig(ldapConfig())
                .build();
    }

    static BlueprintPreparationObject bpObjectWithThreeHostAndLdapRangerConfigured() {
        return getPreparedBuilder()
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(RdsType.RANGER))))
                .withLdapConfig(ldapConfig())
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenKerberosPresentedThenKerberosShouldConfigured() {
        GeneralClusterConfigs configWithGateWay = generalClusterConfigs();
        configWithGateWay.setGatewayInstanceMetadataPresented(true);
        Set<HostgroupView> hostGroupsView = new HashSet<>();
        HostgroupView hg1 = new HostgroupView("master", 0, GATEWAY, 2);
        HostgroupView hg2 = new HostgroupView("slave_1", 0, CORE, 2);
        hostGroupsView.add(hg1);
        hostGroupsView.add(hg2);
        return getPreparedBuilder("master", "slave_1")
                .withGeneralClusterConfigs(configWithGateWay)
                .withHostgroupViews(hostGroupsView)
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withKerberosConfig(kerberosConfig())
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWithZepelinAndHdp26PresentedThenZeppelinShouldConfigured() {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWithZepelinAndHdp25PresentedThenZeppelinShouldConfigured() {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.5", "HDP"))
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenNifiAndHdfPresentedThenHdfShouldConfigured() {
        return getPreparedBuilder("master", "slave_1")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDF"))
                .withHdfConfigs(new HdfConfigs("<property name=\"Node Identity 10.0.0.1\">CN=10.0.0.1, OU=NIFI</property>",
                        "<property name=\"Node Identity 10.0.0.1\">CN=10.0.0.1, OU=NIFI</property>",
                        "<property name=\"Node Identity 10.0.0.1\">CN=10.0.0.1, OU=NIFI</property>", Optional.empty()))
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenNifiAndHdfAndLdapPresentedThenHdfShouldConfigured() {
        return getPreparedBuilder("master", "slave_1")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDF"))
                .withLdapConfig(ldapConfig())
                .withHdfConfigs(new HdfConfigs("<property name=\"Node Identity 10.0.0.1\">CN=10.0.0.1, OU=NIFI</property>",
                        "<property name=\"Node Identity 10.0.0.1\">CN=10.0.0.1, OU=NIFI</property>",
                        "<property name=\"Node Identity 10.0.0.1\">CN=10.0.0.1, OU=NIFI</property>", Optional.empty()))
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenHiveInteractivePresentedTheLlapShouldConfigured() {
        GeneralClusterConfigs conf = generalClusterConfigs();
        conf.setNodeCount(5);
        return Builder.builder()
                .withGeneralClusterConfigs(conf)
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withHostgroups(getHostGroups("master"))
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenNothingSpecialThere() {
        GeneralClusterConfigs conf = generalClusterConfigs();
        conf.setNodeCount(5);
        return Builder.builder()
                .withGeneralClusterConfigs(conf)
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withHostgroups(getHostGroups("master", "slave_1"))
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenLdapConfiguredWithRdsRanger() {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(RdsType.RANGER))))
                .withLdapConfig(ldapConfig())
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenLdapConfiguredWithOracleRdsRanger() {
        return getPreparedBuilder()
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(RdsType.RANGER, DatabaseVendor.ORACLE11))))
                .withLdapConfig(ldapConfig())
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenRdsConfiguredWithRdsOozie() {
        return getPreparedBuilder()
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(RdsType.OOZIE))))
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenWebhcatConfigured() {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenSharedServiceConfigured() {
        GeneralClusterConfigs configs = generalClusterConfigs();
        return Builder.builder()
                .withHostgroups(getHostGroups("master", "worker", "compute"))
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(Sets.newHashSet(rdsConfig(RdsType.RANGER), rdsConfig(RdsType.HIVE)))
                .withLdapConfig(ldapConfig())
                .withGeneralClusterConfigs(configs)
                .withSharedServiceConfigs(datalakeSharedServiceConfig().get())
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenDefaultBlueprintConfigured() {
        GeneralClusterConfigs configs = generalClusterConfigs();

        Set<HostGroup> groups = getHostGroups("master", "worker", "compute");
        for (HostGroup hostGroup : groups) {
            hostGroup.getConstraint().getInstanceGroup().getTemplate().setVolumeCount(5);
        }
        return Builder.builder()
                .withHostgroups(groups)
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(Sets.newHashSet(rdsConfig(RdsType.RANGER), rdsConfig(RdsType.HIVE)))
                .withLdapConfig(ldapConfig())
                .withGeneralClusterConfigs(configs)
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenDlmBlueprintConfigured() throws IOException {
        GeneralClusterConfigs configs = generalClusterConfigs();
        TestFile testFile = getTestFile(getFileName(BLUEPRINT_UPDATER_TEST_INPUTS, "dlm"));
        return Builder.builder()
                .withHostgroups(getHostGroups("master", "worker", "compute"))
                .withBlueprintView(generalBlueprintView(testFile.getFileContent(), "2.6", "HDP"))
                .withRdsConfigs(Sets.newHashSet(rdsConfig(RdsType.BEACON)))
                .withLdapConfig(ldapConfig())
                .withGateway(gatewayEnabled())
                .withGeneralClusterConfigs(configs)
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenDuplicatedStorageLocationKey(String inputFileName) throws IOException {
        GeneralClusterConfigs configs = generalClusterConfigs();
        TestFile testFile = getTestFile(getFileName(BLUEPRINT_UPDATER_TEST_INPUTS, inputFileName));
        return Builder.builder()
                .withHostgroups(getHostGroups("master"))
                .withBlueprintView(generalBlueprintView(testFile.getFileContent(), "2.6", "HDP"))
                .withGeneralClusterConfigs(configs)
                .withFileSystemConfigurationView(new S3FileSystemConfigurationsView(new S3FileSystem(), duplicatedLocationsKey(), false))
                .build();
    }

    private static Collection<StorageLocationView> duplicatedLocationsKey() {
        StorageLocationView storageLocationView = new StorageLocationView(TestUtil.storageLocation("hive-site", 0));
        StorageLocationView storageLocationView2 = new StorageLocationView(TestUtil.storageLocation("hive-site", 1));
        StorageLocationView storageLocationView3 = new StorageLocationView(TestUtil.storageLocation("core-site", 1));
        return Arrays.asList(storageLocationView, storageLocationView2, storageLocationView3);
    }

    static BlueprintPreparationObject blueprintObjectWhenCustomPropertiesBlueprintConfigured() {
        Map<String, Object> customProperties = new HashMap<>();

        customProperties.put("hadoop.security.group.mapping.ldap.url", "10.1.1.2");
        customProperties.put("hadoop.security.group.mapping.ldap.bind.user", "AppleBob");
        customProperties.put("hadoop.security.group.mapping.ldap.bind.password", "Password123!@");
        customProperties.put("hadoop.security.group.mapping.ldap.userbase", "dn");

        Map<String, Object> customPropertiesInner = new HashMap<>();

        customPropertiesInner.put("hadoop.security.group.mapping.ldap.url1", "10.1.1.2");
        customPropertiesInner.put("hadoop.security.group.mapping.ldap.bind.user1", "AppleBob");
        customPropertiesInner.put("hadoop.security.group.mapping.ldap.bind.password1", "Password123!@");
        customPropertiesInner.put("hadoop.security.group.mapping.ldap.userbase1", "dn");

        customProperties.put("custom-core-site", customPropertiesInner);

        GeneralClusterConfigs configs = generalClusterConfigs();
        return Builder.builder()
                .withHostgroups(getHostGroups("master", "slave_1"))
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withGeneralClusterConfigs(configs)
                .withCustomInputs(customProperties)
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenLdapAndDruidRdsConfigured() {
        return getPreparedBuilder()
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(RdsType.DRUID))))
                .withLdapConfig(ldapConfig())
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenAtlasPresentedShouldConfigured() {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenAtlasAndLdapPresentedThenBothShouldConfigured() {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withLdapConfig(ldapConfig())
                .build();
    }

    static BlueprintPreparationObject blueprintObjectForHbaseConfigurationForTwoHosts() {
        return getPreparedBuilder("master", "slave_1")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withHdfConfigs(new HdfConfigs("<property name=\"Node Identity 10.0.0.1\">CN=10.0.0.1, OU=NIFI</property>",
                        "<property name=\"Node Identity 10.0.0.1\">CN=10.0.0.1, OU=NIFI</property>",
                        "<property name=\"Node Identity 10.0.0.1\">CN=10.0.0.1, OU=NIFI</property>", Optional.empty()))
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhereExecutioTypeHasConfiguredAsContainer() {
        GeneralClusterConfigs configs = generalClusterConfigs();
        configs.setExecutorType(ExecutorType.CONTAINER);
        return Builder.builder()
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withHostgroups(getHostGroups("master", "slave_1"))
                .withGeneralClusterConfigs(configs)
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhereSmartSenseHasConfigured() {
        SmartSenseSubscription smartSenseSubscription = new SmartSenseSubscription();
        smartSenseSubscription.setSubscriptionId("A-99900000-C-00000000");
        return getPreparedBuilder("master", "slave_1")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withSmartSenseSubscription(smartSenseSubscription)
                .build();
    }

    static Builder getPreparedBuilder(String... hostNames) {
        return Builder.builder()
                .withGeneralClusterConfigs(generalClusterConfigs())
                .withHostgroups(hostNames.length == 0 ? getHostGroups("master", "worker", "compute") : getHostGroups(hostNames));
    }

    private static Set<HostGroup> getHostGroups(String... names) {
        Set<HostGroup> hostGroups = new HashSet<>(names.length);
        for (String name : names) {
            hostGroups.add(hostGroup(name));
        }
        return hostGroups;
    }

    static TestFile getTestFile(String fileName) throws IOException {
        return new TestFile(new File(fileName).toPath(), FileReaderUtils.readFileFromClasspath(fileName));
    }

    static String getFileName(String folder, String filename) {
        return folder + '/' + filename + ".bp";
    }

    private static Optional<SharedServiceConfigsView> datalakeSharedServiceConfig() {
        SharedServiceConfigsView sharedServiceConfigsView = new SharedServiceConfigsView();
        sharedServiceConfigsView.setDatalakeCluster(true);
        return Optional.of(sharedServiceConfigsView);
    }
}