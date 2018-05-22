package com.sequenceiq.cloudbreak.blueprint.moduletest;

import static com.sequenceiq.cloudbreak.TestUtil.gatewayEnabled;
import static com.sequenceiq.cloudbreak.TestUtil.hostGroup;
import static com.sequenceiq.cloudbreak.TestUtil.kerberosConfig;
import static com.sequenceiq.cloudbreak.TestUtil.ldapConfig;
import static com.sequenceiq.cloudbreak.TestUtil.rdsConfig;
import static com.sequenceiq.cloudbreak.blueprint.filesystem.BlueprintTestUtil.generalBlueprintView;
import static com.sequenceiq.cloudbreak.blueprint.filesystem.BlueprintTestUtil.generalClusterConfigs;
import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModulTest.BLUEPRINT_UPDATER_TEST_INPUTS;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.model.ExecutorType;
import com.sequenceiq.cloudbreak.api.model.rds.RdsType;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.nifi.HdfConfigs;
import com.sequenceiq.cloudbreak.blueprint.template.views.SharedServiceConfigsView;
import com.sequenceiq.cloudbreak.blueprint.templates.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.blueprint.testrepeater.TestFile;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

class BlueprintModulTestModelProvider {

    private BlueprintModulTestModelProvider() {
    }

    static BlueprintPreparationObject blueprintObjectWhenHiveAndRdsPresentedThenRdsHiveMetastoreShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(RdsType.HIVE))))
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenRangerAndRdsPresentedThenRdsRangerShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(RdsType.RANGER))))
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenDruidAndRdsPresentedThenRdsDruidShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder()
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(RdsType.DRUID))))
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenSuperSetAndRdsPresentedThenRdsDruidShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder()
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(RdsType.SUPERSET))))
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenDruidSuperSetAndRdsPresentedThenRdsDruidShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder()
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(RdsType.SUPERSET))))
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenLdapPresentedThenRangerAndHadoopLdapShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withLdapConfig(ldapConfig())
                .build();
    }

    static BlueprintPreparationObject bpObjectWithThreeHostAndLdapRangerConfigured() throws JsonProcessingException {
        return getPreparedBuilder()
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(RdsType.RANGER))))
                .withLdapConfig(ldapConfig())
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenKerberosPresentedThenKerberosShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder("master", "slave_1")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withKerberosConfig(kerberosConfig())
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWithZepelinAndHdp26PresentedThenZeppelinShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWithZepelinAndHdp25PresentedThenZeppelinShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.5", "HDP"))
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenNifiAndHdfPresentedThenHdfShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder("master", "slave_1")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDF"))
                .withHdfConfigs(new HdfConfigs("<property name=\"Node Identity 10.0.0.1\">CN=10.0.0.1, OU=NIFI</property>",
                    "<property name=\"Node Identity 10.0.0.1\">CN=10.0.0.1, OU=NIFI</property>", Optional.empty()))
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenHiveInteractivePresentedTheLlapShouldConfigured() throws JsonProcessingException {
        GeneralClusterConfigs conf = generalClusterConfigs();
        conf.setNodeCount(5);
        return BlueprintPreparationObject.Builder.builder()
                .withGeneralClusterConfigs(conf)
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withHostgroups(getHostGroups("master"))
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenNothingSpecialThere() throws JsonProcessingException {
        GeneralClusterConfigs conf = generalClusterConfigs();
        conf.setNodeCount(5);
        return BlueprintPreparationObject.Builder.builder()
                .withGeneralClusterConfigs(conf)
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withHostgroups(getHostGroups("master", "slave_1"))
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenLdapConfiguredWithRdsRanger() throws JsonProcessingException {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(RdsType.RANGER))))
                .withLdapConfig(ldapConfig())
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenLdapConfiguredWithOracleRdsRanger() throws JsonProcessingException {
        return getPreparedBuilder()
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(RdsType.RANGER, DatabaseVendor.ORACLE11))))
                .withLdapConfig(ldapConfig())
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenRdsConfiguredWithRdsOozie() throws JsonProcessingException {
        return getPreparedBuilder()
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(RdsType.OOZIE))))
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenWebhcatConfigured() throws JsonProcessingException {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenSharedServiceConfigured() throws JsonProcessingException {
        GeneralClusterConfigs configs = generalClusterConfigs();
        return BlueprintPreparationObject.Builder.builder()
                .withHostgroups(getHostGroups("master", "worker", "compute"))
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(Sets.newHashSet(rdsConfig(RdsType.RANGER), rdsConfig(RdsType.HIVE)))
                .withLdapConfig(ldapConfig())
                .withGeneralClusterConfigs(configs)
                .withSharedServiceConfigs(datalakeSharedServiceConfig().get())
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenDefaultBlueprintConfigured() throws JsonProcessingException {
        GeneralClusterConfigs configs = generalClusterConfigs();
        return BlueprintPreparationObject.Builder.builder()
                .withHostgroups(getHostGroups("master", "worker", "compute"))
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(Sets.newHashSet(rdsConfig(RdsType.RANGER), rdsConfig(RdsType.HIVE)))
                .withLdapConfig(ldapConfig())
                .withGeneralClusterConfigs(configs)
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenDlmBlueprintConfigured(String inputFileName) throws IOException {
        GeneralClusterConfigs configs = generalClusterConfigs();
        TestFile testFile = getTestFile(getFileName(BLUEPRINT_UPDATER_TEST_INPUTS, inputFileName));
        return BlueprintPreparationObject.Builder.builder()
                .withHostgroups(getHostGroups("master", "worker", "compute"))
                .withBlueprintView(generalBlueprintView(testFile.getFileContent(), "2.6", "HDP"))
                .withRdsConfigs(Sets.newHashSet(rdsConfig(RdsType.BEACON)))
                .withLdapConfig(ldapConfig())
                .withGateway(gatewayEnabled())
                .withGeneralClusterConfigs(configs)
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenCustomPropertiesBlueprintConfigured() throws IOException {
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
        return BlueprintPreparationObject.Builder.builder()
                .withHostgroups(getHostGroups("master", "slave_1"))
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withGeneralClusterConfigs(configs)
                .withCustomInputs(customProperties)
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenLdapAndDruidRdsConfigured() throws JsonProcessingException {
        return getPreparedBuilder()
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(RdsType.DRUID))))
                .withLdapConfig(ldapConfig())
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenAtlasPresentedShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhenAtlasAndLdapPresentedThenBothShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withLdapConfig(ldapConfig())
                .build();
    }

    static BlueprintPreparationObject blueprintObjectForHbaseConfigurationForTwoHosts() throws JsonProcessingException {
        return getPreparedBuilder("master", "slave_1")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withHdfConfigs(new HdfConfigs("<property name=\"Node Identity 10.0.0.1\">CN=10.0.0.1, OU=NIFI</property>",
                    "<property name=\"Node Identity 10.0.0.1\">CN=10.0.0.1, OU=NIFI</property>", Optional.empty()))
                .build();
    }

    static BlueprintPreparationObject blueprintObjectWhereExecutioTypeHasConfiguredAsContainer() throws JsonProcessingException {
        GeneralClusterConfigs configs = generalClusterConfigs();
        configs.setExecutorType(ExecutorType.CONTAINER);
        return BlueprintPreparationObject.Builder.builder()
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withHostgroups(getHostGroups("master", "slave_1"))
                .withGeneralClusterConfigs(configs)
                .build();
    }

    static BlueprintPreparationObject   blueprintObjectWhereSmartSenseHasConfigured() throws JsonProcessingException {
        return getPreparedBuilder("master", "slave_1")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withSmartSenseSubscriptionId("A-99900000-C-00000000")
                .build();
    }

    static BlueprintPreparationObject.Builder getPreparedBuilder(String... hostNames) throws JsonProcessingException {
        return BlueprintPreparationObject.Builder.builder()
                .withGeneralClusterConfigs(generalClusterConfigs())
                .withHostgroups(hostNames.length == 0 ? getHostGroups() : getHostGroups(hostNames));
    }

    private static Set<HostGroup> getHostGroups() {
        return getHostGroups("master", "worker", "compute");
    }

    private static Set<HostGroup> getHostGroups(String... names) {
        final Set<HostGroup> hostGroups = new HashSet<>(names.length);
        for (String name : names) {
            hostGroups.add(hostGroup(name));
        }
        return hostGroups;
    }

    public static TestFile getTestFile(String fileName) throws IOException {
        return new TestFile(new File(fileName).toPath(), FileReaderUtils.readFileFromClasspath(fileName));
    }

    public static String getFileName(String folder, String filename) {
        return folder + "/" + filename + ".bp";
    }

    private static Optional<SharedServiceConfigsView> datalakeSharedServiceConfig() {
        SharedServiceConfigsView sharedServiceConfigsView = new SharedServiceConfigsView();
        sharedServiceConfigsView.setDatalakeCluster(true);
        return Optional.of(sharedServiceConfigsView);
    }
}