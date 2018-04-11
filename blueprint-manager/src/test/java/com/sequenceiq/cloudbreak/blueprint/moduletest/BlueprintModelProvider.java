package com.sequenceiq.cloudbreak.blueprint.moduletest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.ExecutorType;
import com.sequenceiq.cloudbreak.api.model.rds.RdsType;
import com.sequenceiq.cloudbreak.blueprint.filesystem.BlueprintTestUtil;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.templateprocessor.nifi.HdfConfigs;
import com.sequenceiq.cloudbreak.templateprocessor.processor.PreparationObject;
import com.sequenceiq.cloudbreak.templateprocessor.templates.GeneralClusterConfigs;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

class BlueprintModelProvider {

    private BlueprintModelProvider() {
    }

    static PreparationObject blueprintObjectWhenHiveAndRdsPresentedThenRdsHiveMetastoreShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder()
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(TestUtil.rdsConfig(RdsType.HIVE))))
                .build();
    }

    static PreparationObject blueprintObjectWhenRangerAndRdsPresentedThenRdsRangerShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder()
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(TestUtil.rdsConfig(RdsType.RANGER))))
                .build();
    }

    static PreparationObject blueprintObjectWhenDruidAndRdsPresentedThenRdsDruidShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder()
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(TestUtil.rdsConfig(RdsType.DRUID))))
                .build();
    }

    static PreparationObject blueprintObjectWhenLdapPresentedThenRangerAndHadoopLdapShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder()
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withLdapConfig(TestUtil.ldapConfig())
                .build();
    }

    static PreparationObject bpObjectWithThreeHostAndLdapRangerConfigured() throws JsonProcessingException {
        return getPreparedBuilder()
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(TestUtil.rdsConfig(RdsType.RANGER))))
                .withLdapConfig(TestUtil.ldapConfig())
                .build();
    }

    static PreparationObject blueprintObjectWhenKerberosPresentedThenKerberosShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder("master", "slave_1")
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withKerberosConfig(TestUtil.kerberosConfig())
                .build();
    }

    static PreparationObject blueprintObjectWithZepelinAndHdp26PresentedThenZeppelinShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder("master", "slave_1")
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .build();
    }

    static PreparationObject blueprintObjectWithZepelinAndHdp25PresentedThenZeppelinShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder("master", "slave_1")
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.5", "HDP"))
                .build();
    }

    static PreparationObject blueprintObjectWhenNifiAndHdfPresentedThenHdfShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder("master", "slave_1")
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDF"))
                .withHdfConfigs(new HdfConfigs("<property name=\"Node Identity 10.0.0.1\">CN=10.0.0.1, OU=NIFI</property>", Optional.empty()))
                .build();
    }

    static PreparationObject blueprintObjectWhenHiveInteractivePresentedTheLlapShouldConfigured() throws JsonProcessingException {
        GeneralClusterConfigs conf = BlueprintTestUtil.generalClusterConfigs();
        conf.setNodeCount(5);
        return PreparationObject.Builder.builder()
                .withGeneralClusterConfigs(conf)
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withHostgroups(getHostGroups("master", "slave_1"))
                .withGateway(TestUtil.gateway())
                .build();
    }

    static PreparationObject blueprintObjectWhenNothingSpecialThere() throws JsonProcessingException {
        GeneralClusterConfigs conf = BlueprintTestUtil.generalClusterConfigs();
        conf.setNodeCount(5);
        return PreparationObject.Builder.builder()
                .withGeneralClusterConfigs(conf)
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withHostgroups(getHostGroups("master", "slave_1"))
                .withGateway(TestUtil.gateway())
                .build();
    }

    static PreparationObject blueprintObjectWhenLdapConfiguredWithRdsRanger() throws JsonProcessingException {
        return getPreparedBuilder()
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(TestUtil.rdsConfig(RdsType.RANGER))))
                .withLdapConfig(TestUtil.ldapConfig())
                .build();
    }

    static PreparationObject blueprintObjectWhenRdsConfiguredWithRdsOozie() throws JsonProcessingException {
        return getPreparedBuilder()
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(TestUtil.rdsConfig(RdsType.OOZIE))))
                .build();
    }

    static PreparationObject blueprintObjectWhenWebhcatConfigured() throws JsonProcessingException {
        return getPreparedBuilder("master")
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .build();
    }

    static PreparationObject blueprintObjectWhenLdapAndDruidRdsConfigured() throws JsonProcessingException {
        return getPreparedBuilder()
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(TestUtil.rdsConfig(RdsType.DRUID))))
                .withLdapConfig(TestUtil.ldapConfig())
                .build();
    }

    static PreparationObject blueprintObjectWhenAtlasPresentedShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder("master", "slave_1")
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .build();
    }

    static PreparationObject blueprintObjectWhenAtlasAndLdapPresentedThenBothShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder("master", "slave_1")
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withLdapConfig(TestUtil.ldapConfig())
                .build();
    }

    static PreparationObject blueprintObjectForHbaseConfigurationForTwoHosts() throws JsonProcessingException {
        return getPreparedBuilder("master", "slave_1")
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withHdfConfigs(new HdfConfigs("<property name=\"Node Identity 10.0.0.1\">CN=10.0.0.1, OU=NIFI</property>", Optional.empty()))
                .build();
    }

    static PreparationObject blueprintObjectWhereExecutioTypeHasConfiguredAsContainer() throws JsonProcessingException {
        GeneralClusterConfigs configs = BlueprintTestUtil.generalClusterConfigs();
        configs.setExecutorType(ExecutorType.CONTAINER);
        return PreparationObject.Builder.builder()
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withHostgroups(getHostGroups("master", "slave_1"))
                .withGeneralClusterConfigs(configs)
                .withGateway(TestUtil.gateway())
                .build();
    }

    static PreparationObject   blueprintObjectWhereSmartSenseHasConfigured() throws JsonProcessingException {
        return getPreparedBuilder("master", "slave_1")
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withSmartSenseSubscriptionId("A-99900000-C-00000000")
                .build();
    }

    static PreparationObject.Builder getPreparedBuilder(String... hostNames) throws JsonProcessingException {
        return PreparationObject.Builder.builder()
                .withGeneralClusterConfigs(BlueprintTestUtil.generalClusterConfigs())
                .withHostgroups(hostNames.length == 0 ? getHostGroups() : getHostGroups(hostNames))
                .withGateway(TestUtil.gateway());
    }

    private static Set<HostGroup> getHostGroups() {
        return getHostGroups("master", "worker", "compute");
    }

    private static Set<HostGroup> getHostGroups(String... names) {
        final Set<HostGroup> hostGroups = new HashSet<>(names.length);
        for (String name : names) {
            hostGroups.add(TestUtil.hostGroup(name));
        }
        return hostGroups;
    }

}