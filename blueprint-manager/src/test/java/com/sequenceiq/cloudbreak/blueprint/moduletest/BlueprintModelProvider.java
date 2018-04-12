package com.sequenceiq.cloudbreak.blueprint.moduletest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.ExecutorType;
import com.sequenceiq.cloudbreak.api.model.rds.RdsType;
import com.sequenceiq.cloudbreak.blueprint.filesystem.BlueprintTestUtil;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.template.processor.nifi.HdfConfigs;
import com.sequenceiq.cloudbreak.template.processor.processor.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.processor.templates.GeneralClusterConfigs;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

class BlueprintModelProvider {

    private BlueprintModelProvider() {
    }

    static TemplatePreparationObject blueprintObjectWhenHiveAndRdsPresentedThenRdsHiveMetastoreShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder()
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(TestUtil.rdsConfig(RdsType.HIVE))))
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenRangerAndRdsPresentedThenRdsRangerShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder()
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(TestUtil.rdsConfig(RdsType.RANGER))))
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenDruidAndRdsPresentedThenRdsDruidShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder()
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(TestUtil.rdsConfig(RdsType.DRUID))))
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenLdapPresentedThenRangerAndHadoopLdapShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder()
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withLdapConfig(TestUtil.ldapConfig())
                .build();
    }

    static TemplatePreparationObject bpObjectWithThreeHostAndLdapRangerConfigured() throws JsonProcessingException {
        return getPreparedBuilder()
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(TestUtil.rdsConfig(RdsType.RANGER))))
                .withLdapConfig(TestUtil.ldapConfig())
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenKerberosPresentedThenKerberosShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder("master", "slave_1")
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withKerberosConfig(TestUtil.kerberosConfig())
                .build();
    }

    static TemplatePreparationObject blueprintObjectWithZepelinAndHdp26PresentedThenZeppelinShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder("master", "slave_1")
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .build();
    }

    static TemplatePreparationObject blueprintObjectWithZepelinAndHdp25PresentedThenZeppelinShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder("master", "slave_1")
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.5", "HDP"))
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenNifiAndHdfPresentedThenHdfShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder("master", "slave_1")
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDF"))
                .withHdfConfigs(new HdfConfigs("<property name=\"Node Identity 10.0.0.1\">CN=10.0.0.1, OU=NIFI</property>", Optional.empty()))
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenHiveInteractivePresentedTheLlapShouldConfigured() throws JsonProcessingException {
        GeneralClusterConfigs conf = BlueprintTestUtil.generalClusterConfigs();
        conf.setNodeCount(5);
        return TemplatePreparationObject.Builder.builder()
                .withGeneralClusterConfigs(conf)
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withHostgroups(getHostGroups("master", "slave_1"))
                .withGateway(TestUtil.gateway())
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenNothingSpecialThere() throws JsonProcessingException {
        GeneralClusterConfigs conf = BlueprintTestUtil.generalClusterConfigs();
        conf.setNodeCount(5);
        return TemplatePreparationObject.Builder.builder()
                .withGeneralClusterConfigs(conf)
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withHostgroups(getHostGroups("master", "slave_1"))
                .withGateway(TestUtil.gateway())
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenLdapConfiguredWithRdsRanger() throws JsonProcessingException {
        return getPreparedBuilder()
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(TestUtil.rdsConfig(RdsType.RANGER))))
                .withLdapConfig(TestUtil.ldapConfig())
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenRdsConfiguredWithRdsOozie() throws JsonProcessingException {
        return getPreparedBuilder()
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(TestUtil.rdsConfig(RdsType.OOZIE))))
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenWebhcatConfigured() throws JsonProcessingException {
        return getPreparedBuilder("master")
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenSharedServiceConfigured() throws JsonProcessingException {
        GeneralClusterConfigs configs = BlueprintTestUtil.generalClusterConfigs();
        return TemplatePreparationObject.Builder.builder()
                .withHostgroups(getHostGroups("master", "worker", "compute"))
                .withGateway(TestUtil.gateway())
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(RdsType.RANGER), TestUtil.rdsConfig(RdsType.HIVE)))
                .withLdapConfig(TestUtil.ldapConfig())
                .withGeneralClusterConfigs(configs)
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenLdapAndDruidRdsConfigured() throws JsonProcessingException {
        return getPreparedBuilder()
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(TestUtil.rdsConfig(RdsType.DRUID))))
                .withLdapConfig(TestUtil.ldapConfig())
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenAtlasPresentedShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder("master", "slave_1")
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhenAtlasAndLdapPresentedThenBothShouldConfigured() throws JsonProcessingException {
        return getPreparedBuilder("master", "slave_1")
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withLdapConfig(TestUtil.ldapConfig())
                .build();
    }

    static TemplatePreparationObject blueprintObjectForHbaseConfigurationForTwoHosts() throws JsonProcessingException {
        return getPreparedBuilder("master", "slave_1")
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withHdfConfigs(new HdfConfigs("<property name=\"Node Identity 10.0.0.1\">CN=10.0.0.1, OU=NIFI</property>", Optional.empty()))
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhereExecutioTypeHasConfiguredAsContainer() throws JsonProcessingException {
        GeneralClusterConfigs configs = BlueprintTestUtil.generalClusterConfigs();
        configs.setExecutorType(ExecutorType.CONTAINER);
        return TemplatePreparationObject.Builder.builder()
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withHostgroups(getHostGroups("master", "slave_1"))
                .withGeneralClusterConfigs(configs)
                .withGateway(TestUtil.gateway())
                .build();
    }

    static TemplatePreparationObject blueprintObjectWhereSmartSenseHasConfigured() throws JsonProcessingException {
        return getPreparedBuilder("master", "slave_1")
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView("", "2.6", "HDP"))
                .withSmartSenseSubscriptionId("A-99900000-C-00000000")
                .build();
    }

    static TemplatePreparationObject.Builder getPreparedBuilder(String... hostNames) throws JsonProcessingException {
        return TemplatePreparationObject.Builder.builder()
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