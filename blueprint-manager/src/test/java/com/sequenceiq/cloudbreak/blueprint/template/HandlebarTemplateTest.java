package com.sequenceiq.cloudbreak.blueprint.template;

import static com.sequenceiq.cloudbreak.TestUtil.ldapConfig;
import static com.sequenceiq.cloudbreak.blueprint.filesystem.BlueprintTestUtil.adlsFileSystemConfiguration;
import static com.sequenceiq.cloudbreak.blueprint.filesystem.BlueprintTestUtil.emptyStorageLocationViews;
import static com.sequenceiq.cloudbreak.blueprint.filesystem.BlueprintTestUtil.gcsFileSystemConfiguration;
import static com.sequenceiq.cloudbreak.blueprint.filesystem.BlueprintTestUtil.s3FileSystemConfiguration;
import static com.sequenceiq.cloudbreak.blueprint.filesystem.BlueprintTestUtil.storageLocationViews;
import static com.sequenceiq.cloudbreak.blueprint.filesystem.BlueprintTestUtil.wasbSecureFileSystemConfiguration;
import static com.sequenceiq.cloudbreak.blueprint.filesystem.BlueprintTestUtil.wasbUnSecureFileSystemConfiguration;
import static com.sequenceiq.cloudbreak.util.FileReaderUtils.readFileFromClasspath;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.model.ExecutorType;
import com.sequenceiq.cloudbreak.api.model.rds.RdsType;
import com.sequenceiq.cloudbreak.blueprint.nifi.HdfConfigs;
import com.sequenceiq.cloudbreak.blueprint.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.blueprint.template.views.SharedServiceConfigsView;
import com.sequenceiq.cloudbreak.blueprint.templates.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.common.model.OrchestratorType;
import com.sequenceiq.cloudbreak.domain.RDSConfig;

@RunWith(Parameterized.class)
public class HandlebarTemplateTest {

    private Handlebars handlebars = HandlebarUtils.handlebars();

    private String input;

    private String output;

    private Map<String, Object> model;

    public HandlebarTemplateTest(String input, String output, Map<String, Object> model) {
        this.input = input;
        this.output = output;
        this.model = model;
    }

    @Parameterized.Parameters(name = "{index}: templateTest {0} with handlebar where the expected file is  {1}")
    public static Iterable<Object[]> data() throws JsonProcessingException {
        return Arrays.asList(new Object[][]{

                // HADOOP
                {"blueprints/configurations/hadoop/ldap.handlebars", "configurations/hadoop/hadoop-with-ldap.json",
                        ldapConfigWhenLdapPresentedThenShouldReturnWithLdapConfig()},
                {"blueprints/configurations/hadoop/ldap.handlebars", "configurations/hadoop/hadoop-without-ldap.json",
                        withoutLdapConfigWhenLdapNotPresentedThenShouldReturnWithoutLdapConfig()},
                {"blueprints/configurations/hadoop/global.handlebars", "configurations/hadoop/global.json",
                        objectWithoutEverything()},

                // ADLS
                {"blueprints/configurations/filesystem/adls.handlebars", "configurations/filesystem/adls.json",
                        adlsNotDefaultFileSystemConfigs()},
                {"blueprints/configurations/filesystem/adls.handlebars", "configurations/filesystem/adls-default.json",
                        adlsFileSystemConfigsWithStorageLocation()},

                // WASB
                {"blueprints/configurations/filesystem/wasb.handlebars", "configurations/filesystem/wasb.json",
                        wasbNotDefaultFileSystemConfigs()},
                {"blueprints/configurations/filesystem/wasb.handlebars", "configurations/filesystem/wasb-secure-default.json",
                        wasbSecureFileSystemConfigsWithStorageLocations()},
                {"blueprints/configurations/filesystem/wasb.handlebars", "configurations/filesystem/wasb-unsecure-default.json",
                        wasbUnSecureDefaultFileSystemConfigsWithStorageLocations()},

                // GCS
                {"blueprints/configurations/filesystem/gcs.handlebars", "configurations/filesystem/gcs.json",
                        gcsFileSystemConfigs()},
                {"blueprints/configurations/filesystem/gcs.handlebars", "configurations/filesystem/gcs-default.json",
                        gcsFileSystemConfigsWithStorageLocations()},

                // S3
                {"blueprints/configurations/filesystem/s3.handlebars", "configurations/filesystem/s3.json",
                        s3FileSystemConfigsWithStorageLocations()},

                // LLAP
                {"blueprints/configurations/llap/global.handlebars", "configurations/llap/global.json",
                        llapObjectWhenNodeCountPresented()},

                // NIFI
                {"blueprints/configurations/nifi/global.handlebars", "configurations/nifi/global-with-hdf-nifitargets.json",
                        nifiConfigWhenHdfPresentedThenShouldReturnWithNifiConfig(false)},
                {"blueprints/configurations/nifi/global.handlebars", "configurations/nifi/global-with-hdf-full.json",
                        nifiConfigWhenHdfPresentedThenShouldReturnWithNifiConfig(true)},
                {"blueprints/configurations/nifi/ldap.handlebars", "configurations/nifi/ldap.json",
                        nifiConfigWhenHdfAndLdapPresentedThenShouldReturnWithNifiAndLdapConfig(false)},
                {"blueprints/configurations/nifi/ldap.handlebars", "configurations/nifi/without-ldap.json",
                        nifiConfigWhenHdfPresentedThenShouldReturnWithNifiConfig(true)},

                // NIFI_REGISTRY
                {"blueprints/configurations/nifi_registry/global.handlebars", "configurations/nifi_registry/global-with-hdf-nifitargets.json",
                        nifiConfigWhenHdfPresentedThenShouldReturnWithNifiConfig(false)},
                {"blueprints/configurations/nifi_registry/global.handlebars", "configurations/nifi_registry/global-with-hdf-full.json",
                        nifiConfigWhenHdfPresentedThenShouldReturnWithNifiConfig(true)},
                {"blueprints/configurations/nifi_registry/ldap.handlebars", "configurations/nifi_registry/ldap.json",
                        nifiConfigWhenHdfAndLdapPresentedThenShouldReturnWithNifiAndLdapConfig(false)},
                {"blueprints/configurations/nifi_registry/ldap.handlebars", "configurations/nifi_registry/without-ldap.json",
                        nifiConfigWhenHdfPresentedThenShouldReturnWithNifiConfig(true)},

                // YARN
                {"blueprints/configurations/yarn/global.handlebars", "configurations/yarn/global-without-container.json",
                        objectContainerExecutorIsFalseThenShouldReturnWithoutContainerConfigs()},
                {"blueprints/configurations/yarn/global.handlebars", "configurations/yarn/global-with-container.json",
                        objectContainerExecutorIsTrueThenShouldReturnWithContainerConfigs()},

                // ZEPPELIN
                {"blueprints/configurations/zeppelin/global.handlebars", "configurations/zeppelin/global-with-2_5.json",
                        zeppelinWhenStackVersionIs25ThenShouldReturnWithZeppelinEnvConfigs()},
                {"blueprints/configurations/zeppelin/global.handlebars", "configurations/zeppelin/global-without-2_5.json",
                        zeppelinWhenStackVersionIsNot25ThenShouldReturnWithZeppelinShiroIniConfigs()},

                // OOZIE
                {"blueprints/configurations/oozie/rds.handlebars", "configurations/oozie/oozie-with-postgres-rds.json",
                        oozieWhenRdsPresentedThenShouldReturnWithPostgresRdsConfigs()},
                {"blueprints/configurations/oozie/rds.handlebars", "configurations/oozie/oozie-with-mysql-rds.json",
                        oozieWhenRdsPresentedThenShouldReturnWithMySQLRdsConfigs()},
                {"blueprints/configurations/oozie/rds.handlebars", "configurations/oozie/oozie-with-oracle11-rds.json",
                        oozieWhenRdsPresentedThenShouldReturnWithOracle11RdsConfigs()},
                {"blueprints/configurations/oozie/rds.handlebars", "configurations/oozie/oozie-with-oracle12-rds.json",
                        oozieWhenRdsPresentedThenShouldReturnWithOracle12RdsConfigs()},

                {"blueprints/configurations/oozie/rds.handlebars", "configurations/oozie/oozie-without-rds.json",
                        objectWithoutEverything()},

                // WEBHCAT
                {"blueprints/configurations/webhcat/global.handlebars", "configurations/webhcat/webhcat.json",
                        objectWithoutEverything()},

                // DRUID SUPERSET
                {"blueprints/configurations/druid/rds.handlebars", "configurations/druid/druid-without-rds.json",
                        druidWithoutRdsConfigWhenRdsNotPresentedThenShouldReturnWithoutRdsConfig()},
                {"blueprints/configurations/druid_superset/rds.handlebars", "configurations/druid_superset/druid-without-rds.json",
                        druidSupersetWithoutRdsConfigWhenRdsNotPresentedThenShouldReturnWithoutRdsConfig()},
                {"blueprints/configurations/superset/rds.handlebars", "configurations/superset/superset-with-rds.json",
                        supersetRdsConfigWhenRdsPresentedThenShouldReturnWithRdsConfig()},
                {"blueprints/configurations/superset/rds.handlebars", "configurations/superset/superset-without-rds.json",
                        supersetWithoutRdsConfigWhenRdsNotPresentedThenShouldReturnWithoutRdsConfig()},
                {"blueprints/configurations/druid_superset/rds.handlebars", "configurations/druid_superset/druid-with-mysql-rds.json",
                        druidSupersetRdsConfigWhenRdsPresentedThenShouldReturnWithMySQLRdsConfig()},
                {"blueprints/configurations/druid/rds.handlebars", "configurations/druid/druid-with-postgres-rds.json",
                        druidRdsConfigWhenRdsPresentedThenShouldReturnWithPostgresRdsConfig()},
                {"blueprints/configurations/druid/rds.handlebars", "configurations/druid/druid-with-mysql-rds.json",
                        druidRdsConfigWhenRdsPresentedThenShouldReturnWithMySQLRdsConfig()},

                // HIVE_METASTORE
                {"blueprints/configurations/hive_metastore/shared_service.handlebars", "configurations/hive_metastore/shared-service-attached.json",
                        sSConfigWhenNoSSAndDatalakePresentedThenShouldReturnWithoutSSDatalakeConfig()},
                {"blueprints/configurations/hive_metastore/rds.handlebars", "configurations/hive_metastore/hive-with-rds.json",
                        hiveRdsConfigWhenRdsPresentedThenShouldReturnWithRdsConfig()},
                {"blueprints/configurations/hive_metastore/rds.handlebars", "configurations/hive_metastore/hive-without-rds.json",
                        objectWithoutEverything()},
                {"blueprints/configurations/hive_metastore/rds.handlebars", "configurations/hive_metastore/hive-with-postgres-rds.json",
                        hiveRdsConfigWhenRdsPresentedThenShouldReturnWithPotgresRdsConfig()},
                {"blueprints/configurations/hive_metastore/rds.handlebars", "configurations/hive_metastore/hive-with-oracle11-rds.json",
                        hiveRdsConfigWhenRdsPresentedThenShouldReturnWithOracle11RdsConfig()},
                {"blueprints/configurations/hive_metastore/rds.handlebars", "configurations/hive_metastore/hive-with-oracle12-rds.json",
                        hiveRdsConfigWhenRdsPresentedThenShouldReturnWithOracle12RdsConfig()},
                {"blueprints/configurations/hive_metastore/rds.handlebars", "configurations/hive_metastore/hive-with-mysql-rds.json",
                        hiveRdsConfigWhenRdsPresentedThenShouldReturnWithMySQLRdsConfig()},

                // HIVE_SERVER
                {"blueprints/configurations/hive_server/global.handlebars", "configurations/hive_server/hive-server-without-config.json",
                        objectWithoutEverything()},
                {"blueprints/configurations/hive_server/global.handlebars", "configurations/hive_server/hive-server-with-ranger.json",
                        withRangerAdmin()},
                {"blueprints/configurations/hive_server/ldap.handlebars", "configurations/hive_server/hive-with-ldap.json",
                        hiveWhenLdapPresentedThenShouldReturnWithLdapConfigs()},
                {"blueprints/configurations/hive_server/ldap.handlebars", "configurations/hive_server/hive-without-ldap.json",
                        hiveWhenLdapNotPresentedThenShouldReturnWithoutLdapConfigs()},

                // HIVE_SERVER_INTERACTIVE
                {"blueprints/configurations/hive_server_interactive/ldap.handlebars", "configurations/hive_server_interactive/hive-with-ldap.json",
                        hiveWhenLdapPresentedThenShouldReturnWithLdapConfigs()},
                {"blueprints/configurations/hive_server_interactive/ldap.handlebars", "configurations/hive_server_interactive/hive-without-ldap.json",
                        hiveWhenLdapNotPresentedThenShouldReturnWithoutLdapConfigs()},

                // DP_ROFILER_AGENT
                {"blueprints/configurations/dp_profiler/global.handlebars", "configurations/dp_profiler/profiler.json",
                        objectWithoutEverything()},

                // RANGER_ADMIN
                {"blueprints/configurations/ranger/gateway.handlebars", "configurations/ranger/enable-gateway.json",
                        enabledGateway()},
                {"blueprints/configurations/ranger/gateway.handlebars", "configurations/ranger/disable-gateway.json",
                        objectWithoutEverything()},
                {"blueprints/configurations/ranger/shared_service.handlebars", "configurations/ranger/shared-service-datalake.json",
                        sSConfigWhenSSAndDatalakePresentedThenShouldReturnWithSSDatalakeConfig()},
                {"blueprints/configurations/ranger/shared_service.handlebars", "configurations/ranger/shared-service-no-datalake.json",
                        sSConfigWhenNoSSAndDatalakePresentedThenShouldReturnWithoutSSDatalakeConfig()},
                {"blueprints/configurations/ranger/global.handlebars", "configurations/ranger/global.json",
                        objectWithoutEverything()},
                {"blueprints/configurations/ranger/ldap.handlebars", "configurations/ranger/ranger-with-ldap.json",
                        ldapConfigWhenLdapPresentedThenShouldReturnWithLdapConfig()},
                {"blueprints/configurations/ranger_usersync/ldap.handlebars", "configurations/ranger/ranger-usersync-with-ldap.json",
                        ldapConfigWhenLdapPresentedThenShouldReturnWithLdapConfig()},
                {"blueprints/configurations/ranger/ldap.handlebars", "configurations/ranger/ranger-without-ldap.json",
                        withoutLdapConfigWhenLdapNotPresentedThenShouldReturnWithoutLdapConfig()},
                {"blueprints/configurations/ranger/rds.handlebars", "configurations/ranger/ranger-without-rds.json",
                        objectWithoutEverything()},
                {"blueprints/configurations/ranger/rds.handlebars", "configurations/ranger/ranger-with-postgres-rds.json",
                        rangerRdsConfigWhenRdsPresentedThenShouldReturnWithPostgresRdsConfig()},
                {"blueprints/configurations/ranger/rds.handlebars", "configurations/ranger/ranger-with-mysql-rds.json",
                        rangerRdsConfigWhenRdsPresentedThenShouldReturnWithMySQLRdsConfig()},
                {"blueprints/configurations/ranger/rds.handlebars", "configurations/ranger/ranger-with-oracle11-rds.json",
                        rangerRdsConfigWhenRdsPresentedThenShouldReturnWitOracle11hRdsConfig()},
                {"blueprints/configurations/ranger/rds.handlebars", "configurations/ranger/ranger-with-oracle12-rds.json",
                        rangerRdsConfigWhenRdsPresentedThenShouldReturnWitOracle12hRdsConfig()},

                // BEACON
                {"blueprints/configurations/beacon/gateway.handlebars", "configurations/beacon/enable-gateway.json",
                        enabledGateway()},
                {"blueprints/configurations/beacon/gateway.handlebars", "configurations/beacon/disable-gateway.json",
                        objectWithoutEverything()},
                {"blueprints/configurations/beacon/rds.handlebars", "configurations/beacon/beacon-with-rds.json",
                        beaconWhenRdsPresentedThenShouldReturnWithRdsConfigs()},
                {"blueprints/configurations/beacon/rds.handlebars", "configurations/beacon/beacon-without-rds.json",
                        objectWithoutEverything()},

                // ATLAS
                {"blueprints/configurations/atlas/gateway.handlebars", "configurations/atlas/enable-gateway.json",
                        enabledGateway()},
                {"blueprints/configurations/atlas/gateway.handlebars", "configurations/atlas/disable-gateway.json",
                        objectWithoutEverything()},
                {"blueprints/configurations/atlas/gateway.handlebars", "configurations/atlas/enable-gateway-with-ranger.json",
                        enabledGatewayWithRanger()},
                {"blueprints/configurations/atlas/ldap.handlebars", "configurations/atlas/atlas-with-ldap.json",
                        ldapConfigWhenLdapPresentedThenShouldReturnWithLdapConfig()},
                {"blueprints/configurations/atlas/ldap.handlebars", "configurations/atlas/atlas-without-ldap.json",
                        withoutLdapConfigWhenLdapNotPresentedThenShouldReturnWithoutLdapConfig()}

        });
    }

    @Test
    public void test() throws IOException {
        String actual = compileTemplate(input, model);
        String expected = readExpectedTemplate(output);
        String message = String.format("expected: [%s] %nactual: [%s] %n", expected, actual);

        Assert.assertEquals(message, expected, actual);
    }

    public static Map<String, Object> objectWithoutEverything() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("cloudbreak123!");

        return new BlueprintTemplateModelContextBuilder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> adlsNotDefaultFileSystemConfigs() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("cloudbreak123!");

        return new BlueprintTemplateModelContextBuilder()
                .withFileSystemConfigs(adlsFileSystemConfiguration(emptyStorageLocationViews()))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> adlsFileSystemConfigsWithStorageLocation() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("cloudbreak123!");

        return new BlueprintTemplateModelContextBuilder()
                .withFileSystemConfigs(adlsFileSystemConfiguration(storageLocationViews()))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> wasbNotDefaultFileSystemConfigs() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("cloudbreak123!");

        return new BlueprintTemplateModelContextBuilder()
                .withFileSystemConfigs(wasbSecureFileSystemConfiguration(emptyStorageLocationViews()))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> wasbSecureFileSystemConfigsWithStorageLocations() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("cloudbreak123!");

        return new BlueprintTemplateModelContextBuilder()
                .withFileSystemConfigs(wasbSecureFileSystemConfiguration(storageLocationViews()))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> wasbUnSecureDefaultFileSystemConfigsWithStorageLocations() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("cloudbreak123!");

        return new BlueprintTemplateModelContextBuilder()
                .withFileSystemConfigs(wasbUnSecureFileSystemConfiguration(storageLocationViews()))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> gcsFileSystemConfigs() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("cloudbreak123!");

        return new BlueprintTemplateModelContextBuilder()
                .withFileSystemConfigs(gcsFileSystemConfiguration(emptyStorageLocationViews()))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> gcsFileSystemConfigsWithStorageLocations() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("cloudbreak123!");

        return new BlueprintTemplateModelContextBuilder()
                .withFileSystemConfigs(gcsFileSystemConfiguration(storageLocationViews()))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> s3FileSystemConfigsWithStorageLocations() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("cloudbreak123!");

        return new BlueprintTemplateModelContextBuilder()
                .withFileSystemConfigs(s3FileSystemConfiguration(storageLocationViews()))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> beaconWhenRdsPresentedThenShouldReturnWithRdsConfigs() {
        return new BlueprintTemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(RdsType.BEACON)))
                .build();
    }

    public static Map<String, Object> zeppelinWhenStackVersionIsNot25ThenShouldReturnWithZeppelinShiroIniConfigs() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("blueprints_basics_zeppelin_shiro_ini_content", "testshiroini");

        BlueprintView blueprintView = new BlueprintView("blueprintText", "2.6", "HDP");

        return new BlueprintTemplateModelContextBuilder()
                .withBlueprintView(blueprintView)
                .withFixInputs(properties)
                .build();
    }

    public static Map<String, Object> zeppelinWhenStackVersionIs25ThenShouldReturnWithZeppelinEnvConfigs() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("blueprints_basics_zeppelin_shiro_ini_content", "testshiroini");

        BlueprintView blueprintView = new BlueprintView("blueprintText", "2.5", "HDP");

        return new BlueprintTemplateModelContextBuilder()
                .withBlueprintView(blueprintView)
                .withFixInputs(properties)
                .build();
    }

    public static Map<String, Object> objectContainerExecutorIsTrueThenShouldReturnWithContainerConfigs() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setOrchestratorType(OrchestratorType.CONTAINER);
        generalClusterConfigs.setExecutorType(ExecutorType.CONTAINER);
        return new BlueprintTemplateModelContextBuilder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> objectContainerExecutorIsFalseThenShouldReturnWithoutContainerConfigs() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setOrchestratorType(OrchestratorType.HOST);

        return new BlueprintTemplateModelContextBuilder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> llapObjectWhenNodeCountPresented() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setNodeCount(6);

        return new BlueprintTemplateModelContextBuilder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> ldapConfigWhenLdapPresentedThenShouldReturnWithLdapConfig() throws JsonProcessingException {
        return new BlueprintTemplateModelContextBuilder()
                .withLdap(ldapConfig())
                .withGateway(TestUtil.gatewayEnabled())
                .build();
    }

    public static Map<String, Object> enabledGateway() throws JsonProcessingException {
        return new BlueprintTemplateModelContextBuilder()
                .withGateway(TestUtil.gatewayEnabled())
                .build();
    }

    public static Map<String, Object> enabledGatewayWithRanger() throws JsonProcessingException {
        return new BlueprintTemplateModelContextBuilder()
                .withGateway(TestUtil.gatewayEnabled())
                .withComponents(Sets.newHashSet("RANGER_ADMIN"))
                .build();
    }

    public static Map<String, Object> withRangerAdmin() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("cloudbreak123!");

        return new BlueprintTemplateModelContextBuilder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withComponents(Sets.newHashSet("RANGER_ADMIN"))
                .build();
    }

    public static Map<String, Object> withoutLdapConfigWhenLdapNotPresentedThenShouldReturnWithoutLdapConfig() {
        return new BlueprintTemplateModelContextBuilder()
                .build();
    }

    public static Map<String, Object> druidSupersetWithoutRdsConfigWhenRdsNotPresentedThenShouldReturnWithoutRdsConfig() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("adminPassword");
        generalClusterConfigs.setUserName("lastname");
        generalClusterConfigs.setIdentityUserEmail("admin@example.com");

        return new BlueprintTemplateModelContextBuilder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> druidWithoutRdsConfigWhenRdsNotPresentedThenShouldReturnWithoutRdsConfig() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("adminPassword");
        generalClusterConfigs.setUserName("lastname");
        generalClusterConfigs.setIdentityUserEmail("admin@example.com");

        return new BlueprintTemplateModelContextBuilder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> supersetRdsConfigWhenRdsPresentedThenShouldReturnWithRdsConfig() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("adminPassword");
        generalClusterConfigs.setUserName("lastname");
        generalClusterConfigs.setIdentityUserEmail("admin@example.com");

        return new BlueprintTemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(RdsType.SUPERSET)))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> supersetWithoutRdsConfigWhenRdsNotPresentedThenShouldReturnWithoutRdsConfig() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("adminPassword");
        generalClusterConfigs.setUserName("lastname");
        generalClusterConfigs.setIdentityUserEmail("admin@example.com");

        return new BlueprintTemplateModelContextBuilder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> nifiConfigWhenHdfPresentedThenShouldReturnWithNifiConfig(boolean withProxyHost) {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("adminPassword");
        generalClusterConfigs.setUserName("lastname");
        generalClusterConfigs.setIdentityUserEmail("admin@example.com");

        BlueprintView blueprintView = new BlueprintView("blueprintText", "2.6", "HDF");

        return new BlueprintTemplateModelContextBuilder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withHdfConfigs(new HdfConfigs("nifigtargets", "nifigtargets", "nifigtargets",
                        withProxyHost ? Optional.of("nifiproxyhost") : Optional.empty()))
                .withBlueprintView(blueprintView)
                .build();
    }

    public static Map<String, Object> nifiConfigWhenHdfAndLdapPresentedThenShouldReturnWithNifiAndLdapConfig(boolean withProxyHost) {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("adminPassword");
        generalClusterConfigs.setUserName("lastname");
        generalClusterConfigs.setIdentityUserEmail("admin@example.com");

        BlueprintView blueprintView = new BlueprintView("blueprintText", "2.6", "HDF");

        Map<String, Object> properties = new HashMap<>();
        properties.put("blueprints_basics_nifi_registry_identity_providers", "<test>");
        properties.put("blueprints_basics_nifi_registry_authorizers", "<test1>");
        properties.put("blueprints_basics_nifi_identity_providers", "<test>");
        properties.put("blueprints_basics_nifi_authorizers", "<test1>");

        return new BlueprintTemplateModelContextBuilder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withLdap(ldapConfig())
                .withFixInputs(properties)
                .withHdfConfigs(new HdfConfigs("nifigtargets", "nifigtargets", "nifigtargets",
                        withProxyHost ? Optional.of("nifiproxyhost") : Optional.empty()))
                .withBlueprintView(blueprintView)
                .build();
    }

    public static Map<String, Object> nifiConfigWhenHdfNotPresentedThenShouldReturnWithNotNifiConfig() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("adminPassword");
        generalClusterConfigs.setUserName("lastname");
        generalClusterConfigs.setIdentityUserEmail("admin@example.com");

        BlueprintView blueprintView = new BlueprintView("blueprintText", "2.6", "HDP");

        return new BlueprintTemplateModelContextBuilder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withHdfConfigs(new HdfConfigs("nifigtargets", "nifigtargets", "nifigtargets", Optional.empty()))
                .withBlueprintView(blueprintView)
                .build();
    }

    public static Map<String, Object> hiveRdsConfigWhenRdsPresentedThenShouldReturnWithRdsConfig() {
        return new BlueprintTemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(RdsType.HIVE)))
                .build();
    }

    private static Object hiveWhenLdapPresentedThenShouldReturnWithLdapConfigs() {
        return new BlueprintTemplateModelContextBuilder()
                .withLdap(ldapConfig())
                .build();
    }

    private static Object hiveWhenLdapNotPresentedThenShouldReturnWithoutLdapConfigs() {
        return new BlueprintTemplateModelContextBuilder()
                .build();
    }

    public static Map<String, Object> sSConfigWhenSSAndDatalakePresentedThenShouldReturnWithSSDatalakeConfig() throws JsonProcessingException {
        return new BlueprintTemplateModelContextBuilder()
                .withSharedServiceConfigs(datalakeSharedServiceConfig().get())
                .build();
    }

    public static Map<String, Object> sSConfigWhenNoSSAndDatalakePresentedThenShouldReturnWithoutSSDatalakeConfig() throws JsonProcessingException {
        Map<String, Object> fixInputs = new HashMap<>();
        fixInputs.put("REMOTE_CLUSTER_NAME", "datalake-1");
        fixInputs.put("policymgr_external_url", "10.1.1.1:6080");
        return new BlueprintTemplateModelContextBuilder()
                .withSharedServiceConfigs(attachedClusterSharedServiceConfig().get())
                .withFixInputs(fixInputs)
                .build();
    }

    public static Map<String, Object> rangerRdsConfigWhenRdsPresentedThenShouldReturnWithPostgresRdsConfig() throws JsonProcessingException {
        RDSConfig rdsConfig = TestUtil.rdsConfig(RdsType.RANGER, DatabaseVendor.POSTGRES);
        return new BlueprintTemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(rdsConfig))
                .build();
    }

    public static Map<String, Object> rangerRdsConfigWhenRdsPresentedThenShouldReturnWithMySQLRdsConfig() throws JsonProcessingException {
        RDSConfig rdsConfig = TestUtil.rdsConfig(RdsType.RANGER, DatabaseVendor.MYSQL);
        return new BlueprintTemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(rdsConfig))
                .build();
    }

    public static Map<String, Object> rangerRdsConfigWhenRdsPresentedThenShouldReturnWitOracle11hRdsConfig() throws JsonProcessingException {
        RDSConfig rdsConfig = TestUtil.rdsConfig(RdsType.RANGER, DatabaseVendor.ORACLE11);
        return new BlueprintTemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(rdsConfig))
                .build();
    }

    public static Map<String, Object> rangerRdsConfigWhenRdsPresentedThenShouldReturnWitOracle12hRdsConfig() throws JsonProcessingException {
        RDSConfig rdsConfig = TestUtil.rdsConfig(RdsType.RANGER, DatabaseVendor.ORACLE12);
        return new BlueprintTemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(rdsConfig))
                .build();
    }

    public static Map<String, Object> hiveRdsConfigWhenRdsPresentedThenShouldReturnWithPotgresRdsConfig() {
        return new BlueprintTemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(RdsType.HIVE, DatabaseVendor.POSTGRES)))
                .build();
    }

    public static Map<String, Object> hiveRdsConfigWhenRdsPresentedThenShouldReturnWithOracle11RdsConfig() {
        return new BlueprintTemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(RdsType.HIVE, DatabaseVendor.ORACLE11)))
                .build();
    }

    public static Map<String, Object> hiveRdsConfigWhenRdsPresentedThenShouldReturnWithOracle12RdsConfig() {
        return new BlueprintTemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(RdsType.HIVE, DatabaseVendor.ORACLE12)))
                .build();
    }

    public static Map<String, Object> hiveRdsConfigWhenRdsPresentedThenShouldReturnWithMySQLRdsConfig() {
        return new BlueprintTemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(RdsType.HIVE, DatabaseVendor.MYSQL)))
                .build();
    }

    public static Map<String, Object> oozieWhenRdsPresentedThenShouldReturnWithPostgresRdsConfigs() {
        return new BlueprintTemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(RdsType.OOZIE, DatabaseVendor.POSTGRES)))
                .build();
    }

    public static Map<String, Object> oozieWhenRdsPresentedThenShouldReturnWithOracle11RdsConfigs() {
        return new BlueprintTemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(RdsType.OOZIE, DatabaseVendor.ORACLE11)))
                .build();
    }

    public static Map<String, Object> oozieWhenRdsPresentedThenShouldReturnWithOracle12RdsConfigs() {
        return new BlueprintTemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(RdsType.OOZIE, DatabaseVendor.ORACLE12)))
                .build();
    }

    public static Map<String, Object> oozieWhenRdsPresentedThenShouldReturnWithMySQLRdsConfigs() {
        return new BlueprintTemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(RdsType.OOZIE, DatabaseVendor.MYSQL)))
                .build();
    }

    public static Map<String, Object> druidRdsConfigWhenRdsPresentedThenShouldReturnWithPostgresRdsConfig() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("adminPassword");
        generalClusterConfigs.setUserName("lastname");
        generalClusterConfigs.setIdentityUserEmail("admin@example.com");

        return new BlueprintTemplateModelContextBuilder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(RdsType.DRUID, DatabaseVendor.POSTGRES)))
                .build();
    }

    public static Map<String, Object> druidSupersetRdsConfigWhenRdsPresentedThenShouldReturnWithMySQLRdsConfig() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("adminPassword");
        generalClusterConfigs.setUserName("lastname");
        generalClusterConfigs.setIdentityUserEmail("admin@example.com");

        return new BlueprintTemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(RdsType.SUPERSET, DatabaseVendor.MYSQL)))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> druidRdsConfigWhenRdsPresentedThenShouldReturnWithMySQLRdsConfig() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("adminPassword");
        generalClusterConfigs.setUserName("lastname");
        generalClusterConfigs.setIdentityUserEmail("admin@example.com");

        return new BlueprintTemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(RdsType.DRUID, DatabaseVendor.MYSQL)))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    private String compileTemplate(String sourceFile, Map<String, Object> model) {
        String result;
        try {
            Template template = handlebars.compileInline(readSourceTemplate(sourceFile), "{{{", "}}}");
            result = template.apply(model);
        } catch (IOException e) {
            result = "";
        }
        return result;
    }

    private String readExpectedTemplate(String file) throws IOException {
        return readFileFromClasspath(String.format("handlebar/%s", file));
    }

    private String readSourceTemplate(String file) throws IOException {
        return readFileFromClasspath(String.format("%s", file));
    }

    private static Optional<SharedServiceConfigsView> datalakeSharedServiceConfig() {
        SharedServiceConfigsView sharedServiceConfigsView = new SharedServiceConfigsView();
        sharedServiceConfigsView.setDatalakeCluster(true);
        return Optional.of(sharedServiceConfigsView);
    }

    private static Optional<SharedServiceConfigsView> attachedClusterSharedServiceConfig() {
        SharedServiceConfigsView sharedServiceConfigsView = new SharedServiceConfigsView();
        sharedServiceConfigsView.setAttachedCluster(true);
        sharedServiceConfigsView.setRangerAdminPassword("cloudbreak123!");
        return Optional.of(sharedServiceConfigsView);
    }
}
