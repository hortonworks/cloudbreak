package com.sequenceiq.cloudbreak.blueprint.moduletest;

import static com.sequenceiq.cloudbreak.TestUtil.adConfig;
import static com.sequenceiq.cloudbreak.TestUtil.gatewayEnabled;
import static com.sequenceiq.cloudbreak.TestUtil.hostGroup;
import static com.sequenceiq.cloudbreak.TestUtil.kerberosConfigMit;
import static com.sequenceiq.cloudbreak.TestUtil.ldapConfig;
import static com.sequenceiq.cloudbreak.TestUtil.rdsConfig;
import static com.sequenceiq.cloudbreak.common.type.InstanceGroupType.CORE;
import static com.sequenceiq.cloudbreak.common.type.InstanceGroupType.GATEWAY;
import static com.sequenceiq.cloudbreak.blueprint.filesystem.BlueprintTestUtil.generalBlueprintView;
import static com.sequenceiq.cloudbreak.blueprint.filesystem.BlueprintTestUtil.generalClusterConfigs;
import static com.sequenceiq.cloudbreak.blueprint.moduletest.AmbariBlueprintModulTest.BLUEPRINT_UPDATER_TEST_INPUTS;

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
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ExecutorType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.blueprint.testrepeater.TestFile;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.common.type.filesystem.S3FileSystem;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.model.HdfConfigs;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

class AmbariBlueprintModulTestModelProvider {

    private AmbariBlueprintModulTestModelProvider() {
    }

    static TemplatePreparationObject blueprintObjectWhenHiveAndRdsPresentedThenRdsHiveMetastoreShouldConfigured() {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(DatabaseType.HIVE))))
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenRangerAndRdsPresentedThenRdsRangerShouldConfigured() {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(DatabaseType.RANGER))))
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenDruidAndRdsPresentedThenRdsDruidShouldConfigured() {
        return getPreparedBuilder()
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(DatabaseType.DRUID))))
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenSuperSetAndRdsPresentedThenRdsDruidShouldConfigured() {
        return getPreparedBuilder()
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(DatabaseType.SUPERSET))))
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenDruidSuperSetAndRdsPresentedThenRdsDruidShouldConfigured() {
        return getPreparedBuilder()
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(DatabaseType.SUPERSET))))
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenLdapPresentedThenRangerAndHadoopLdapShouldConfigured() {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withLdapConfig(ldapConfig(), "cn=admin,dc=example,dc=org", "admin")
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenADPresentedThenRangerAndHadoopADShouldConfigured() {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withLdapConfig(adConfig(), "cn=admin,dc=example,dc=org", "admin")
                .build();
    }

    static TemplatePreparationObject bpObjectWithThreeHostAndLdapRangerConfigured() {
        return getPreparedBuilder()
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(DatabaseType.RANGER))))
                .withLdapConfig(ldapConfig(), "cn=admin,dc=example,dc=org", "admin")
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenKerberosPresentedThenKerberosShouldConfigured() {
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
                .withKerberosConfig(kerberosConfigMit())
                .build();
    }

    static TemplatePreparationObject blueprintObjectWithZepelinAndHdp26PresentedThenZeppelinShouldConfigured() {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .build();
    }

    static TemplatePreparationObject blueprintObjectWithZepelinAndHdp25PresentedThenZeppelinShouldConfigured() {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.5", "HDP"))
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenNifiAndHdfPresentedThenHdfShouldConfigured() {
        return getPreparedBuilder("master", "slave_1")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDF"))
                .withHdfConfigs(new HdfConfigs("<property name=\"Node Identity 10.0.0.1\">CN=10.0.0.1, OU=NIFI</property>",
                        "<property name=\"Node Identity 10.0.0.1\">CN=10.0.0.1, OU=NIFI</property>",
                        "<property name=\"Node Identity 10.0.0.1\">CN=10.0.0.1, OU=NIFI</property>", Optional.empty()))
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenNifiAndHdfAndLdapPresentedThenHdfShouldConfigured() {
        return getPreparedBuilder("master", "slave_1")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDF"))
                .withLdapConfig(ldapConfig(), "cn=admin,dc=example,dc=org", "admin")
                .withHdfConfigs(new HdfConfigs("<property name=\"Node Identity 10.0.0.1\">CN=10.0.0.1, OU=NIFI</property>",
                        "<property name=\"Node Identity 10.0.0.1\">CN=10.0.0.1, OU=NIFI</property>",
                        "<property name=\"Node Identity 10.0.0.1\">CN=10.0.0.1, OU=NIFI</property>", Optional.empty()))
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenHiveInteractivePresentedTheLlapShouldConfigured() {
        GeneralClusterConfigs conf = generalClusterConfigs();
        conf.setNodeCount(5);
        return Builder.builder()
                .withGeneralClusterConfigs(conf)
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withHostgroups(getHostGroups("master"))
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenNothingSpecialThere() {
        GeneralClusterConfigs conf = generalClusterConfigs();
        conf.setNodeCount(5);
        return Builder.builder()
                .withGeneralClusterConfigs(conf)
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withHostgroups(getHostGroups("master", "slave_1"))
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenLdapConfiguredWithRdsRanger() {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(DatabaseType.RANGER))))
                .withLdapConfig(ldapConfig(), "cn=admin,dc=example,dc=org", "admin")
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenADConfiguredWithRdsRanger() {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(DatabaseType.RANGER))))
                .withLdapConfig(adConfig(), "cn=admin,dc=example,dc=org", "admin")
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenLdapConfiguredWithOracleRdsRanger() {
        return getPreparedBuilder()
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(DatabaseType.RANGER, DatabaseVendor.ORACLE11))))
                .withLdapConfig(ldapConfig(), "cn=admin,dc=example,dc=org", "admin")
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenRdsConfiguredWithRdsOozie() {
        return getPreparedBuilder()
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(DatabaseType.OOZIE))))
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenWebhcatConfigured() {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenSharedServiceConfigured() {
        GeneralClusterConfigs configs = generalClusterConfigs();
        return Builder.builder()
                .withHostgroups(getHostGroups("master", "worker", "compute"))
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(Sets.newHashSet(rdsConfig(DatabaseType.RANGER), rdsConfig(DatabaseType.HIVE)))
                .withLdapConfig(ldapConfig(), "cn=admin,dc=example,dc=org", "admin")
                .withGeneralClusterConfigs(configs)
                .withSharedServiceConfigs(datalakeSharedServiceConfig().get())
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenDefaultBlueprintConfigured() {
        GeneralClusterConfigs configs = generalClusterConfigs();

        Set<HostGroup> groups = getHostGroups("master", "worker", "compute");
        for (HostGroup hostGroup : groups) {
            VolumeTemplate volumeTemplate = new VolumeTemplate();
            volumeTemplate.setVolumeCount(5);
            hostGroup.getConstraint().getInstanceGroup().getTemplate().setVolumeTemplates(Sets.newHashSet(volumeTemplate));
        }
        return Builder.builder()
                .withHostgroups(groups)
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(Sets.newHashSet(rdsConfig(DatabaseType.RANGER), rdsConfig(DatabaseType.HIVE)))
                .withLdapConfig(ldapConfig(), "cn=admin,dc=example,dc=org", "admin")
                .withGeneralClusterConfigs(configs)
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenDlmBlueprintConfiguredAndLdap(String inputFileName) throws IOException {
        GeneralClusterConfigs configs = generalClusterConfigs();
        TestFile testFile = getTestFile(getFileName(BLUEPRINT_UPDATER_TEST_INPUTS, "dlm"));
        return Builder.builder()
                .withHostgroups(getHostGroups("master", "worker", "compute"))
                .withBlueprintView(generalBlueprintView(testFile.getFileContent(), "2.6", "HDP"))
                .withRdsConfigs(Sets.newHashSet(rdsConfig(DatabaseType.BEACON)))
                .withLdapConfig(ldapConfig(), "cn=admin,dc=example,dc=org", "admin")
                .withGateway(gatewayEnabled(), "/cb/secret/signkey")
                .withGeneralClusterConfigs(configs)
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenDlmBlueprintConfiguredAndAD(String inputFileName) throws IOException {
        GeneralClusterConfigs configs = generalClusterConfigs();
        TestFile testFile = getTestFile(getFileName(BLUEPRINT_UPDATER_TEST_INPUTS, inputFileName));
        return Builder.builder()
                .withHostgroups(getHostGroups("master", "worker", "compute"))
                .withBlueprintView(generalBlueprintView(testFile.getFileContent(), "2.6", "HDP"))
                .withRdsConfigs(Sets.newHashSet(rdsConfig(DatabaseType.BEACON)))
                .withLdapConfig(adConfig(), "cn=admin,dc=example,dc=org", "admin")
                .withGateway(gatewayEnabled(), "/cb/secret/signkey")
                .withGeneralClusterConfigs(configs)
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenDuplicatedStorageLocationKey(String inputFileName) throws IOException {
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

    static TemplatePreparationObject blueprintObjectWhenCustomPropertiesBlueprintConfigured() {
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

    static TemplatePreparationObject blueprintObjectWhenLdapAndDruidRdsConfigured() {
        return getPreparedBuilder()
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(DatabaseType.DRUID))))
                .withLdapConfig(ldapConfig(), "cn=admin,dc=example,dc=org", "admin")
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenAtlasPresentedShouldConfigured() {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenAtlasAndLdapPresentedThenBothShouldConfigured() {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withLdapConfig(ldapConfig(), "cn=admin,dc=example,dc=org", "admin")
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenAtlasAndADPresentedThenBothShouldConfigured() {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withLdapConfig(adConfig(), "cn=admin,dc=example,dc=org", "admin")
                .build();
    }

    static TemplatePreparationObject blueprintObjectForHbaseConfigurationForTwoHosts() {
        return getPreparedBuilder("master", "slave_1")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withHdfConfigs(new HdfConfigs("<property name=\"Node Identity 10.0.0.1\">CN=10.0.0.1, OU=NIFI</property>",
                        "<property name=\"Node Identity 10.0.0.1\">CN=10.0.0.1, OU=NIFI</property>",
                        "<property name=\"Node Identity 10.0.0.1\">CN=10.0.0.1, OU=NIFI</property>", Optional.empty()))
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhereExecutioTypeHasConfiguredAsContainer() {
        GeneralClusterConfigs configs = generalClusterConfigs();
        configs.setExecutorType(ExecutorType.CONTAINER);
        return Builder.builder()
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withHostgroups(getHostGroups("master", "slave_1"))
                .withGeneralClusterConfigs(configs)
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