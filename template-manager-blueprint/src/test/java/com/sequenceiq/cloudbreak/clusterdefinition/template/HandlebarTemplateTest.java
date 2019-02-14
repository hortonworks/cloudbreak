package com.sequenceiq.cloudbreak.clusterdefinition.template;

import static com.sequenceiq.cloudbreak.TestUtil.adConfig;
import static com.sequenceiq.cloudbreak.TestUtil.ldapConfig;
import static com.sequenceiq.cloudbreak.TestUtil.ldapConfigWithSpecialChars;
import static com.sequenceiq.cloudbreak.template.filesystem.TemplateCoreTestUtil.adlsFileSystemConfiguration;
import static com.sequenceiq.cloudbreak.template.filesystem.TemplateCoreTestUtil.emptyStorageLocationViews;
import static com.sequenceiq.cloudbreak.template.filesystem.TemplateCoreTestUtil.gcsFileSystemConfiguration;
import static com.sequenceiq.cloudbreak.template.filesystem.TemplateCoreTestUtil.s3FileSystemConfiguration;
import static com.sequenceiq.cloudbreak.template.filesystem.TemplateCoreTestUtil.storageLocationViews;
import static com.sequenceiq.cloudbreak.template.filesystem.TemplateCoreTestUtil.storageLocationViewsWithDuplicatedKey;
import static com.sequenceiq.cloudbreak.template.filesystem.TemplateCoreTestUtil.wasbSecureFileSystemConfiguration;
import static com.sequenceiq.cloudbreak.template.filesystem.TemplateCoreTestUtil.wasbUnSecureFileSystemConfiguration;
import static com.sequenceiq.cloudbreak.util.FileReaderUtils.readFileFromClasspath;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ExecutorType;
import com.sequenceiq.cloudbreak.common.model.OrchestratorType;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.template.HandlebarUtils;
import com.sequenceiq.cloudbreak.template.TemplateModelContextBuilder;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.model.HdfConfigs;
import com.sequenceiq.cloudbreak.template.views.ClusterDefinitionView;
import com.sequenceiq.cloudbreak.template.views.LdapView;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;

@RunWith(Parameterized.class)
public class HandlebarTemplateTest {

    private final Handlebars handlebars = HandlebarUtils.handlebars();

    private final String input;

    private final String output;

    private final Map<String, Object> model;

    public HandlebarTemplateTest(String input, String output, Map<String, Object> model) {
        this.input = input;
        this.output = output;
        this.model = model;
    }

    @Parameters(name = "{index}: templateTest {0} with handlebar where the expected file is {1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{

                // HADOOP
                {"blueprints/configurations/hadoop/ldap.handlebars", "configurations/hadoop/hadoop-with-ldap.json",
                        ldapConfigWhenLdapPresentedThenShouldReturnWithLdapConfig()},
                {"blueprints/configurations/hadoop/ldap.handlebars", "configurations/hadoop/hadoop-without-ldap.json",
                        withoutLdapConfigWhenLdapNotPresentedThenShouldReturnWithoutLdapConfig()},
                {"blueprints/configurations/hadoop/ldap.handlebars", "configurations/hadoop/hadoop-without-ldap2.json",
                        ldapConfigWithLdapAndKerberosPresent()},
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
                {"blueprints/configurations/filesystem/s3.handlebars", "configurations/filesystem/s3-duplicated-key.json",
                        s3FileSystemConfigsWithStorageLocationsAndDuplicatedKey()},

                // NIFI
                {"blueprints/configurations/nifi/global.handlebars", "configurations/nifi/global-with-hdf-nifitargets.json",
                        nifiConfigWhenHdfPresentedThenShouldReturnWithNifiConfig(false)},
                {"blueprints/configurations/nifi/global.handlebars", "configurations/nifi/global-with-hdf-full.json",
                        nifiConfigWhenHdfPresentedThenShouldReturnWithNifiConfig(true)},
                {"blueprints/configurations/nifi/ldap.handlebars", "configurations/nifi/ldap.json",
                        nifiConfigWhenHdfAndLdapPresentedThenShouldReturnWithNifiAndLdapConfig(false)},
                {"blueprints/configurations/nifi/ldap.handlebars", "configurations/nifi/without-ldap.json",
                        nifiConfigWhenHdfPresentedThenShouldReturnWithNifiConfig(true)},
                {"blueprints/basics/nifi/authorizers.handlebars", "configurations/nifi/authorizers.xml",
                        nifiWithLdap()},
                {"blueprints/basics/nifi/identity_providers.handlebars", "configurations/nifi/identity_providers.xml",
                        nifiWithLdap()},

                // NIFI_REGISTRY
                {"blueprints/configurations/nifi_registry/global.handlebars", "configurations/nifi_registry/global-with-hdf-nifitargets.json",
                        nifiConfigWhenHdfPresentedThenShouldReturnWithNifiConfig(false)},
                {"blueprints/configurations/nifi_registry/global.handlebars", "configurations/nifi_registry/global-with-hdf-full.json",
                        nifiConfigWhenHdfPresentedThenShouldReturnWithNifiConfig(true)},
                {"blueprints/configurations/nifi_registry/ldap.handlebars", "configurations/nifi_registry/ldap.json",
                        nifiConfigWhenHdfAndLdapPresentedThenShouldReturnWithNifiAndLdapConfig(false)},
                {"blueprints/configurations/nifi_registry/ldap.handlebars", "configurations/nifi_registry/without-ldap.json",
                        nifiConfigWhenHdfPresentedThenShouldReturnWithNifiConfig(true)},
                {"blueprints/basics/nifi_registry/authorizers.handlebars", "configurations/nifi_registry/authorizers.xml",
                        nifiWithLdap()},
                {"blueprints/basics/nifi_registry/identity_providers.handlebars", "configurations/nifi_registry/identity_providers.xml",
                        nifiWithLdap()},

                // REGISTRY
                {"blueprints/configurations/registry/rds.handlebars", "configurations/registry/registry-with-rds.json",
                        registryRdsConfigWhenRdsPresentedThenShouldReturnWithRdsConfig()},
                {"blueprints/configurations/registry/rds.handlebars", "configurations/registry/registry-without-rds.json",
                        registryWithoutRdsConfigWhenRdsNotPresentedThenShouldReturnWithoutRdsConfig()},
                {"blueprints/configurations/registry/rds.handlebars", "configurations/registry/registry-with-postgres-rds.json",
                        registryRdsConfigWhenPostGresDBEngineShouldReturnWithCorrectDatabaseTypeConfig()},

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
                {"blueprints/configurations/hive_server/shared_service.handlebars", "configurations/hive_server/shared-service-attached.json",
                        sSConfigWhenNoSSAndDatalakePresentedThenShouldReturnWithoutSSDatalakeConfig()},
                {"blueprints/configurations/hive_server/shared_service.handlebars", "configurations/hive_server/shared-service-atlas-attached.json",
                        sSConfigWhenAtlasIsPresentedInDatalakeThenShouldReturnWithAtlasAndKafkaConfigs()},

                // HIVE_SERVER_INTERACTIVE
                {"blueprints/configurations/hive_server_interactive/ldap.handlebars", "configurations/hive_server_interactive/hive-with-ldap.json",
                        hiveWhenLdapPresentedThenShouldReturnWithLdapConfigs()},
                {"blueprints/configurations/hive_server_interactive/ldap.handlebars", "configurations/hive_server_interactive/hive-without-ldap.json",
                        hiveWhenLdapNotPresentedThenShouldReturnWithoutLdapConfigs()},
                {"blueprints/configurations/hive_server_interactive/shared_service.handlebars",
                        "configurations/hive_server_interactive/shared-service-attached.json",
                        sSConfigWhenNoSSAndDatalakePresentedThenShouldReturnWithoutSSDatalakeConfig()},
                {"blueprints/configurations/hive_server_interactive/shared_service.handlebars",
                        "configurations/hive_server_interactive/shared-service-atlas-attached.json",
                        sSConfigWhenAtlasIsPresentedInDatalakeThenShouldReturnWithAtlasAndKafkaConfigs()},

                // DP_ROFILER_AGENT
                {"blueprints/configurations/dp_profiler/global.handlebars", "configurations/dp_profiler/profiler.json",
                        objectWithoutEverything()},

                // METRICS_MONITOR
                {"blueprints/configurations/metrics_monitor/shared_service.handlebars", "configurations/metrics_monitor/metrics-monitor.json",
                        sSConfigWhenAttachedWLThenShouldReturnWithAttachedWLConfig()},

                // YARN_CLIENT
                {"blueprints/configurations/yarn_client/shared_service.handlebars", "configurations/yarn_client/yarn-client.json",
                        sSConfigWhenAttachedWLThenShouldReturnWithAttachedWLConfig()},

                // KAFKA_BROKER
                {"blueprints/configurations/kafka_broker/shared_service.handlebars", "configurations/kafka_broker/shared-service-attached.json",
                        sSConfigWhenAttachedWLThenShouldReturnWithAttachedWLConfig()},

                // LOGSEARCH_LOGFEEDER
                {"blueprints/configurations/logsearch_logfeeder/cloud_storage.handlebars", "configurations/logsearch_logfeeder/logsearch-logfeeder-empty.json",
                        logfeederConfigDlNoCloudStorage()},
                {"blueprints/configurations/logsearch_logfeeder/cloud_storage.handlebars", "configurations/logsearch_logfeeder/logsearch-logfeeder-cloud.json",
                        logfeederConfigDlWithCloudStorageNoSolr()},
                {"blueprints/configurations/logsearch_logfeeder/cloud_storage.handlebars", "configurations/logsearch_logfeeder/logsearch-logfeeder-hybrid.json",
                        logfeederConfigDlWithCloudStorageWithSolr()},
                {"blueprints/configurations/logsearch_logfeeder/cloud_storage.handlebars", "configurations/logsearch_logfeeder/logsearch-logfeeder-empty.json",
                        logfeederConfigAttachedWlNoCloudStorage()},
                {"blueprints/configurations/logsearch_logfeeder/cloud_storage.handlebars", "configurations/logsearch_logfeeder/logsearch-logfeeder-cloud.json",
                        logfeederConfigAttachedWlWithCloudStorageNoSolr()},
                {"blueprints/configurations/logsearch_logfeeder/cloud_storage.handlebars", "configurations/logsearch_logfeeder/logsearch-logfeeder-hybrid.json",
                        logfeederConfigAttachedWlWithCloudStorageWithSolr()},
                {"blueprints/configurations/logsearch_logfeeder/cloud_storage.handlebars", "configurations/logsearch_logfeeder/logsearch-logfeeder-empty.json",
                        logfeederConfigStandaloneWlNoCloudStorage()},
                {"blueprints/configurations/logsearch_logfeeder/cloud_storage.handlebars", "configurations/logsearch_logfeeder/logsearch-logfeeder-cloud.json",
                        logfeederConfigStandaloneWlWithCloudStorageNoSolr()},
                {"blueprints/configurations/logsearch_logfeeder/cloud_storage.handlebars", "configurations/logsearch_logfeeder/logsearch-logfeeder-hybrid.json",
                        logfeederConfigStandaloneWlWithCloudStorageWithSolr()},
                {"blueprints/configurations/logsearch_logfeeder/external_solr.handlebars", "configurations/logsearch_logfeeder/logsearch-logfeeder-empty.json",
                        logfeederConfigDlWithCloudStorageWithSolr()},
                {"blueprints/configurations/logsearch_logfeeder/external_solr.handlebars", "configurations/logsearch_logfeeder/logsearch-logfeeder-empty2.json",
                        logfeederConfigAttachedWlWithCloudStorageNoSolr()},
                {"blueprints/configurations/logsearch_logfeeder/external_solr.handlebars",
                        "configurations/logsearch_logfeeder/logsearch-logfeeder-external_solr.json",
                        logfeederConfigAttachedWlWithCloudStorageWithSolr()},
                {"blueprints/configurations/logsearch_logfeeder/external_solr.handlebars",
                        "configurations/logsearch_logfeeder/logsearch-logfeeder-external_solr-krb.json",
                        logfeederConfigAttachedWlWithCloudStorageWithSolrKrb()},
                {"blueprints/configurations/logsearch_logfeeder/external_solr.handlebars", "configurations/logsearch_logfeeder/logsearch-logfeeder-empty.json",
                        logfeederConfigStandaloneWlWithCloudStorageWithSolr()},

                // RANGER_ADMIN
                {"blueprints/configurations/ranger/gateway.handlebars", "configurations/ranger/enable-gateway.json",
                        enabledGateway()},
                {"blueprints/configurations/ranger/gateway.handlebars", "configurations/ranger/disable-gateway.json",
                        objectWithoutEverything()},
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
                {"blueprints/configurations/atlas/gateway.handlebars", "configurations/atlas/enable-gateway-without-sso-and-with-ranger.json",
                        enabledGatewayWithoutSSOAndWithRanger()},
                {"blueprints/configurations/atlas/gateway.handlebars", "configurations/atlas/disable-gateway.json",
                        objectWithoutEverything()},
                {"blueprints/configurations/atlas/gateway.handlebars", "configurations/atlas/enable-gateway-with-ranger.json",
                        enabledGatewayWithRanger()},
                {"blueprints/configurations/atlas/ldap.handlebars", "configurations/atlas/atlas-with-ldap.json",
                        ldapConfigWhenLdapPresentedThenShouldReturnWithLdapConfig()},
                {"blueprints/configurations/atlas/ldap.handlebars", "configurations/atlas/atlas-with-ad.json",
                        ldapConfigWhenLdapPresentedThenShouldReturnWithAdConfig()},
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

        return new TemplateModelContextBuilder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> adlsNotDefaultFileSystemConfigs() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("cloudbreak123!");

        return new TemplateModelContextBuilder()
                .withFileSystemConfigs(adlsFileSystemConfiguration(emptyStorageLocationViews()))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> adlsFileSystemConfigsWithStorageLocation() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("cloudbreak123!");

        return new TemplateModelContextBuilder()
                .withFileSystemConfigs(adlsFileSystemConfiguration(storageLocationViews()))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> wasbNotDefaultFileSystemConfigs() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("cloudbreak123!");

        return new TemplateModelContextBuilder()
                .withFileSystemConfigs(wasbSecureFileSystemConfiguration(emptyStorageLocationViews()))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> wasbSecureFileSystemConfigsWithStorageLocations() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("cloudbreak123!");

        return new TemplateModelContextBuilder()
                .withFileSystemConfigs(wasbSecureFileSystemConfiguration(storageLocationViews()))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> wasbUnSecureDefaultFileSystemConfigsWithStorageLocations() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("cloudbreak123!");

        return new TemplateModelContextBuilder()
                .withFileSystemConfigs(wasbUnSecureFileSystemConfiguration(storageLocationViews()))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> gcsFileSystemConfigs() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("cloudbreak123!");

        return new TemplateModelContextBuilder()
                .withFileSystemConfigs(gcsFileSystemConfiguration(emptyStorageLocationViews()))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> gcsFileSystemConfigsWithStorageLocations() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("cloudbreak123!");

        return new TemplateModelContextBuilder()
                .withFileSystemConfigs(gcsFileSystemConfiguration(storageLocationViews()))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> s3FileSystemConfigsWithStorageLocations() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("cloudbreak123!");

        return new TemplateModelContextBuilder()
                .withFileSystemConfigs(s3FileSystemConfiguration(storageLocationViews()))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> s3FileSystemConfigsWithStorageLocationsAndDuplicatedKey() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("cloudbreak123!");

        return new TemplateModelContextBuilder()
                .withFileSystemConfigs(s3FileSystemConfiguration(storageLocationViewsWithDuplicatedKey()))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> beaconWhenRdsPresentedThenShouldReturnWithRdsConfigs() {
        return new TemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(DatabaseType.BEACON)))
                .build();
    }

    public static Map<String, Object> zeppelinWhenStackVersionIsNot25ThenShouldReturnWithZeppelinShiroIniConfigs() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("blueprints_basics_zeppelin_shiro_ini_content", "testshiroini");

        ClusterDefinitionView clusterDefinitionView = new ClusterDefinitionView("blueprintText", "2.6", "HDP");

        return new TemplateModelContextBuilder()
                .withBlueprintView(clusterDefinitionView)
                .withFixInputs(properties)
                .build();
    }

    public static Map<String, Object> zeppelinWhenStackVersionIs25ThenShouldReturnWithZeppelinEnvConfigs() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("blueprints_basics_zeppelin_shiro_ini_content", "testshiroini");

        ClusterDefinitionView clusterDefinitionView = new ClusterDefinitionView("blueprintText", "2.5", "HDP");

        return new TemplateModelContextBuilder()
                .withBlueprintView(clusterDefinitionView)
                .withFixInputs(properties)
                .build();
    }

    public static Map<String, Object> objectContainerExecutorIsTrueThenShouldReturnWithContainerConfigs() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setOrchestratorType(OrchestratorType.CONTAINER);
        generalClusterConfigs.setExecutorType(ExecutorType.CONTAINER);
        return new TemplateModelContextBuilder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> objectContainerExecutorIsFalseThenShouldReturnWithoutContainerConfigs() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setOrchestratorType(OrchestratorType.HOST);

        return new TemplateModelContextBuilder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> llapObjectWhenNodeCountPresented() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setNodeCount(6);

        return new TemplateModelContextBuilder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> ldapConfigWithLdapAndKerberosPresent() {
        KerberosConfig kerberosConfig = new KerberosConfig();
        kerberosConfig.setName("kerberos-config");
        kerberosConfig.setAdmin("admin");

        return new TemplateModelContextBuilder()
                .withLdap(new LdapView(ldapConfig(), "cn=admin,dc=example,dc=org", "admin"))
                .withGateway(TestUtil.gatewayEnabled(), "/cb/secret/signkey")
                .withKerberos(kerberosConfig)
                .build();
    }

    public static Map<String, Object> ldapConfigWhenLdapPresentedThenShouldReturnWithLdapConfig() {
        return new TemplateModelContextBuilder()
                .withLdap(new LdapView(ldapConfig(), "cn=admin,dc=example,dc=org", "admin"))
                .withGateway(TestUtil.gatewayEnabled(), "/cb/secret/signkey")
                .build();
    }

    public static Map<String, Object> ldapConfigWhenLdapPresentedThenShouldReturnWithAdConfig() {
        return new TemplateModelContextBuilder()
                .withLdap(new LdapView(adConfig(), "cn=admin,dc=example,dc=org", "admin"))
                .withGateway(TestUtil.gatewayEnabled(), "/cb/secret/signkey")
                .build();
    }

    public static Map<String, Object> enabledGateway() {
        return new TemplateModelContextBuilder()
                .withGateway(TestUtil.gatewayEnabled(), "/cb/secret/signkey")
                .build();
    }

    public static Map<String, Object> enabledGatewayWithoutSSOAndWithRanger() {
        return new TemplateModelContextBuilder()
                .withGateway(TestUtil.gatewayEnabledWithoutSSOAndWithRanger(), "/cb/secret/signkey")
                .withComponents(Sets.newHashSet("RANGER_ADMIN"))
                .build();
    }

    public static Map<String, Object> enabledGatewayWithRanger() {
        return new TemplateModelContextBuilder()
                .withGateway(TestUtil.gatewayEnabled(), "/cb/secret/signkey")
                .withComponents(Sets.newHashSet("RANGER_ADMIN"))
                .build();
    }

    public static Map<String, Object> withRangerAdmin() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("cloudbreak123!");

        return new TemplateModelContextBuilder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withComponents(Sets.newHashSet("RANGER_ADMIN"))
                .build();
    }

    public static Map<String, Object> withoutLdapConfigWhenLdapNotPresentedThenShouldReturnWithoutLdapConfig() {
        return new TemplateModelContextBuilder()
                .build();
    }

    public static Map<String, Object> druidSupersetWithoutRdsConfigWhenRdsNotPresentedThenShouldReturnWithoutRdsConfig() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("adminPassword");
        generalClusterConfigs.setUserName("lastname");
        generalClusterConfigs.setIdentityUserEmail("admin@example.com");

        return new TemplateModelContextBuilder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> druidWithoutRdsConfigWhenRdsNotPresentedThenShouldReturnWithoutRdsConfig() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("adminPassword");
        generalClusterConfigs.setUserName("lastname");
        generalClusterConfigs.setIdentityUserEmail("admin@example.com");

        return new TemplateModelContextBuilder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> supersetRdsConfigWhenRdsPresentedThenShouldReturnWithRdsConfig() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("adminPassword");
        generalClusterConfigs.setUserName("lastname");
        generalClusterConfigs.setIdentityUserEmail("admin@example.com");

        return new TemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(DatabaseType.SUPERSET)))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> supersetWithoutRdsConfigWhenRdsNotPresentedThenShouldReturnWithoutRdsConfig() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("adminPassword");
        generalClusterConfigs.setUserName("lastname");
        generalClusterConfigs.setIdentityUserEmail("admin@example.com");

        return new TemplateModelContextBuilder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> nifiConfigWhenHdfPresentedThenShouldReturnWithNifiConfig(boolean withProxyHost) {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("adminPassword");
        generalClusterConfigs.setUserName("lastname");
        generalClusterConfigs.setIdentityUserEmail("admin@example.com");

        ClusterDefinitionView clusterDefinitionView = new ClusterDefinitionView("blueprintText", "2.6", "HDF");

        return new TemplateModelContextBuilder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withHdfConfigs(new HdfConfigs("nifigtargets", "nifigtargets", "nifigtargets",
                        withProxyHost ? Optional.of("nifiproxyhost") : Optional.empty()))
                .withBlueprintView(clusterDefinitionView)
                .build();
    }

    public static Map<String, Object> nifiWithLdap() {
        return new TemplateModelContextBuilder()
                .withLdap(new LdapView(ldapConfigWithSpecialChars(), "cn=admin,dc=example,dc=org", "admin<>char"))
                .withHdfConfigs(new HdfConfigs("<property>nodeEntities</property>",
                        "<property>registryNodeEntities</property>",
                        "<property>nodeUserEntities</property>",
                        Optional.empty()))
                .build();
    }

    public static Map<String, Object> nifiConfigWhenHdfAndLdapPresentedThenShouldReturnWithNifiAndLdapConfig(boolean withProxyHost) {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("adminPassword");
        generalClusterConfigs.setUserName("lastname");
        generalClusterConfigs.setIdentityUserEmail("admin@example.com");

        ClusterDefinitionView clusterDefinitionView = new ClusterDefinitionView("blueprintText", "2.6", "HDF");

        Map<String, Object> properties = new HashMap<>();
        properties.put("blueprints_basics_nifi_registry_identity_providers", "<test>");
        properties.put("blueprints_basics_nifi_registry_authorizers", "<test1>");
        properties.put("blueprints_basics_nifi_identity_providers", "<test>");
        properties.put("blueprints_basics_nifi_authorizers", "<test1>");

        return new TemplateModelContextBuilder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withLdap(new LdapView(ldapConfig(), "cn=admin,dc=example,dc=org", "admin<>char"))
                .withFixInputs(properties)
                .withHdfConfigs(new HdfConfigs("nifigtargets", "nifigtargets", "nifigtargets",
                        withProxyHost ? Optional.of("nifiproxyhost") : Optional.empty()))
                .withBlueprintView(clusterDefinitionView)
                .build();
    }

    public static Map<String, Object> nifiConfigWhenHdfNotPresentedThenShouldReturnWithNotNifiConfig() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("adminPassword");
        generalClusterConfigs.setUserName("lastname");
        generalClusterConfigs.setIdentityUserEmail("admin@example.com");

        ClusterDefinitionView clusterDefinitionView = new ClusterDefinitionView("blueprintText", "2.6", "HDP");

        return new TemplateModelContextBuilder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withHdfConfigs(new HdfConfigs("nifigtargets", "nifigtargets", "nifigtargets", Optional.empty()))
                .withBlueprintView(clusterDefinitionView)
                .build();
    }

    public static Map<String, Object> registryWithoutRdsConfigWhenRdsNotPresentedThenShouldReturnWithoutRdsConfig() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("adminPassword");
        generalClusterConfigs.setUserName("lastname");
        generalClusterConfigs.setIdentityUserEmail("admin@example.com");

        ClusterDefinitionView clusterDefinitionView = new ClusterDefinitionView("blueprintText", "2.6", "HDF");

        return new TemplateModelContextBuilder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withBlueprintView(clusterDefinitionView)
                .build();
    }

    public static Map<String, Object> registryRdsConfigWhenRdsPresentedThenShouldReturnWithRdsConfig() {
        RDSConfig rdsConfig = TestUtil.rdsConfig(DatabaseType.REGISTRY, DatabaseVendor.MYSQL);
        ClusterDefinitionView clusterDefinitionView = new ClusterDefinitionView("blueprintText", "2.6", "HDF");
        return new TemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(rdsConfig))
                .withBlueprintView(clusterDefinitionView)
                .build();
    }

    public static Map<String, Object> registryRdsConfigWhenPostGresDBEngineShouldReturnWithCorrectDatabaseTypeConfig() {
        RDSConfig rdsConfig = TestUtil.rdsConfig(DatabaseType.REGISTRY, DatabaseVendor.POSTGRES);
        ClusterDefinitionView clusterDefinitionView = new ClusterDefinitionView("blueprintText", "2.6", "HDF");
        return new TemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(rdsConfig))
                .withBlueprintView(clusterDefinitionView)
                .build();
    }

    public static Map<String, Object> hiveRdsConfigWhenRdsPresentedThenShouldReturnWithRdsConfig() {
        return new TemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(DatabaseType.HIVE)))
                .build();
    }

    private static Object hiveWhenLdapPresentedThenShouldReturnWithLdapConfigs() {
        return new TemplateModelContextBuilder()
                .withLdap(new LdapView(ldapConfig(), "cn=admin,dc=example,dc=org", "admin<>char"))
                .build();
    }

    private static Object hiveWhenLdapNotPresentedThenShouldReturnWithoutLdapConfigs() {
        return new TemplateModelContextBuilder()
                .build();
    }

    public static Map<String, Object> sSConfigWhenSSAndDatalakePresentedThenShouldReturnWithSSDatalakeConfig() {
        return new TemplateModelContextBuilder()
                .withSharedServiceConfigs(datalakeSharedServiceConfig().get())
                .build();
    }

    public static Map<String, Object> sSConfigWhenNoSSAndDatalakePresentedThenShouldReturnWithoutSSDatalakeConfig() {
        Map<String, Object> fixInputs = new HashMap<>();
        fixInputs.put("remoteClusterName", "datalake-1");
        fixInputs.put("policymgr_external_url", "10.1.1.1:6080");
        fixInputs.put("ranger.audit.solr.zookeepers", "10.1.1.1:2181/infra-solr");

        SharedServiceConfigsView sharedServiceConfigsView = attachedClusterSharedServiceConfig().get();
        Set<String> objects = new HashSet<>();
        objects.add("RANGER_ADMIN");
        sharedServiceConfigsView.setDatalakeComponents(objects);

        return new TemplateModelContextBuilder()
                .withSharedServiceConfigs(sharedServiceConfigsView)
                .withFixInputs(fixInputs)
                .build();
    }

    private static Object sSConfigWhenAttachedWLThenShouldReturnWithAttachedWLConfig() {
        SharedServiceConfigsView sharedServiceConfigsView = attachedClusterSharedServiceConfig().get();
        sharedServiceConfigsView.setDatalakeAmbariFqdn("ambarifqdn");

        return new TemplateModelContextBuilder()
                .withSharedServiceConfigs(sharedServiceConfigsView)
                .build();
    }

    private static Object sSConfigWhenAtlasIsPresentedInDatalakeThenShouldReturnWithAtlasAndKafkaConfigs() {
        Map<String, Object> fixInputs = new HashMap<>();
        fixInputs.put("remoteClusterName", "datalake-1");
        fixInputs.put("policymgr_external_url", "10.1.1.1:6080");
        fixInputs.put("atlas.kafka.bootstrap.servers", "10.1.1.1:6667");
        fixInputs.put("atlas.rest.address", "http://10.1.1.1:21000");
        fixInputs.put("atlas.kafka.security.protocol", "SASL_PLAINTEXT");
        fixInputs.put("atlas.jaas.KafkaClient.option.serviceName", "kafka");
        fixInputs.put("atlas.kafka.sasl.kerberos.service.name", "kafka");
        fixInputs.put("atlas.kafka.zookeeper.connect", "10.1.1.1:2181");
        fixInputs.put("ranger.audit.solr.zookeepers", "10.1.1.1:2181/infra-solr");

        SharedServiceConfigsView sharedServiceConfigsView = attachedClusterSharedServiceConfig().get();
        sharedServiceConfigsView.setDatalakeAmbariIp("10.1.1.1");

        Set<String> objects = new HashSet<>();
        objects.add("KAFKA_BROKER");
        objects.add("ATLAS_SERVER");
        objects.add("RANGER_ADMIN");
        sharedServiceConfigsView.setDatalakeComponents(objects);

        return new TemplateModelContextBuilder()
                .withSharedServiceConfigs(sharedServiceConfigsView)
                .withFixInputs(fixInputs)
                .build();
    }

    public static Map<String, Object> rangerRdsConfigWhenRdsPresentedThenShouldReturnWithPostgresRdsConfig() {
        RDSConfig rdsConfig = TestUtil.rdsConfig(DatabaseType.RANGER, DatabaseVendor.POSTGRES);
        return new TemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(rdsConfig))
                .build();
    }

    public static Map<String, Object> rangerRdsConfigWhenRdsPresentedThenShouldReturnWithMySQLRdsConfig() {
        RDSConfig rdsConfig = TestUtil.rdsConfig(DatabaseType.RANGER, DatabaseVendor.MYSQL);
        return new TemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(rdsConfig))
                .build();
    }

    public static Map<String, Object> rangerRdsConfigWhenRdsPresentedThenShouldReturnWitOracle11hRdsConfig() {
        RDSConfig rdsConfig = TestUtil.rdsConfig(DatabaseType.RANGER, DatabaseVendor.ORACLE11);
        return new TemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(rdsConfig))
                .build();
    }

    public static Map<String, Object> rangerRdsConfigWhenRdsPresentedThenShouldReturnWitOracle12hRdsConfig() {
        RDSConfig rdsConfig = TestUtil.rdsConfig(DatabaseType.RANGER, DatabaseVendor.ORACLE12);
        return new TemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(rdsConfig))
                .build();
    }

    public static Map<String, Object> hiveRdsConfigWhenRdsPresentedThenShouldReturnWithPotgresRdsConfig() {
        return new TemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(DatabaseType.HIVE, DatabaseVendor.POSTGRES)))
                .build();
    }

    public static Map<String, Object> hiveRdsConfigWhenRdsPresentedThenShouldReturnWithOracle11RdsConfig() {
        return new TemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(DatabaseType.HIVE, DatabaseVendor.ORACLE11)))
                .build();
    }

    public static Map<String, Object> hiveRdsConfigWhenRdsPresentedThenShouldReturnWithOracle12RdsConfig() {
        return new TemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(DatabaseType.HIVE, DatabaseVendor.ORACLE12)))
                .build();
    }

    public static Map<String, Object> hiveRdsConfigWhenRdsPresentedThenShouldReturnWithMySQLRdsConfig() {
        return new TemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(DatabaseType.HIVE, DatabaseVendor.MYSQL)))
                .build();
    }

    public static Map<String, Object> oozieWhenRdsPresentedThenShouldReturnWithPostgresRdsConfigs() {
        return new TemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(DatabaseType.OOZIE, DatabaseVendor.POSTGRES)))
                .build();
    }

    public static Map<String, Object> oozieWhenRdsPresentedThenShouldReturnWithOracle11RdsConfigs() {
        return new TemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(DatabaseType.OOZIE, DatabaseVendor.ORACLE11)))
                .build();
    }

    public static Map<String, Object> oozieWhenRdsPresentedThenShouldReturnWithOracle12RdsConfigs() {
        return new TemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(DatabaseType.OOZIE, DatabaseVendor.ORACLE12)))
                .build();
    }

    public static Map<String, Object> oozieWhenRdsPresentedThenShouldReturnWithMySQLRdsConfigs() {
        return new TemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(DatabaseType.OOZIE, DatabaseVendor.MYSQL)))
                .build();
    }

    public static Map<String, Object> druidRdsConfigWhenRdsPresentedThenShouldReturnWithPostgresRdsConfig() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("adminPassword");
        generalClusterConfigs.setUserName("lastname");
        generalClusterConfigs.setIdentityUserEmail("admin@example.com");

        return new TemplateModelContextBuilder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(DatabaseType.DRUID, DatabaseVendor.POSTGRES)))
                .build();
    }

    public static Map<String, Object> druidSupersetRdsConfigWhenRdsPresentedThenShouldReturnWithMySQLRdsConfig() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("adminPassword");
        generalClusterConfigs.setUserName("lastname");
        generalClusterConfigs.setIdentityUserEmail("admin@example.com");

        return new TemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(DatabaseType.SUPERSET, DatabaseVendor.MYSQL)))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    public static Map<String, Object> druidRdsConfigWhenRdsPresentedThenShouldReturnWithMySQLRdsConfig() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPassword("adminPassword");
        generalClusterConfigs.setUserName("lastname");
        generalClusterConfigs.setIdentityUserEmail("admin@example.com");

        return new TemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(DatabaseType.DRUID, DatabaseVendor.MYSQL)))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    private static Object logfeederConfigDlNoCloudStorage() {
        SharedServiceConfigsView sharedServiceConfigsView = datalakeSharedServiceConfig().get();

        return new TemplateModelContextBuilder()
                .withSharedServiceConfigs(sharedServiceConfigsView)
                .build();
    }

    private static Object logfeederConfigDlWithCloudStorageNoSolr() {
        SharedServiceConfigsView sharedServiceConfigsView = datalakeSharedServiceConfig().get();

        return new TemplateModelContextBuilder()
                .withSharedServiceConfigs(sharedServiceConfigsView)
                .withFileSystemConfigs(s3FileSystemConfiguration(storageLocationViews()))
                .build();
    }

    private static Object logfeederConfigDlWithCloudStorageWithSolr() {
        SharedServiceConfigsView sharedServiceConfigsView = datalakeSharedServiceConfig().get();

        return new TemplateModelContextBuilder()
                .withSharedServiceConfigs(sharedServiceConfigsView)
                .withFileSystemConfigs(s3FileSystemConfiguration(storageLocationViews()))
                .withComponents(Set.of("INFRA_SOLR"))
                .build();
    }

    private static Object logfeederConfigAttachedWlNoCloudStorage() {
        SharedServiceConfigsView sharedServiceConfigsView = attachedClusterSharedServiceConfig().get();

        return new TemplateModelContextBuilder()
                .withSharedServiceConfigs(sharedServiceConfigsView)
                .build();
    }

    private static Object logfeederConfigAttachedWlWithCloudStorageNoSolr() {
        SharedServiceConfigsView sharedServiceConfigsView = attachedClusterSharedServiceConfig().get();

        return new TemplateModelContextBuilder()
                .withSharedServiceConfigs(sharedServiceConfigsView)
                .withFileSystemConfigs(s3FileSystemConfiguration(storageLocationViews()))
                .build();
    }

    private static Object logfeederConfigAttachedWlWithCloudStorageWithSolr() {
        SharedServiceConfigsView sharedServiceConfigsView = attachedClusterSharedServiceConfig().get();
        sharedServiceConfigsView.setDatalakeComponents(Set.of("INFRA_SOLR"));
        sharedServiceConfigsView.setDatalakeAmbariFqdn("dl-ambari-host.example.com");

        return new TemplateModelContextBuilder()
                .withSharedServiceConfigs(sharedServiceConfigsView)
                .withFileSystemConfigs(s3FileSystemConfiguration(storageLocationViews()))
                .build();
    }

    private static Object logfeederConfigAttachedWlWithCloudStorageWithSolrKrb() {
        SharedServiceConfigsView sharedServiceConfigsView = attachedClusterSharedServiceConfig().get();
        sharedServiceConfigsView.setDatalakeComponents(Set.of("INFRA_SOLR"));
        sharedServiceConfigsView.setDatalakeAmbariFqdn("dl-ambari-host.example.com");

        return new TemplateModelContextBuilder()
                .withSharedServiceConfigs(sharedServiceConfigsView)
                .withFileSystemConfigs(s3FileSystemConfiguration(storageLocationViews()))
                .withKerberos(TestUtil.kerberosConfigFreeipa())
                .build();
    }

    private static Object logfeederConfigStandaloneWlNoCloudStorage() {
        return new TemplateModelContextBuilder()
                .build();
    }

    private static Object logfeederConfigStandaloneWlWithCloudStorageNoSolr() {
        return new TemplateModelContextBuilder()
                .withFileSystemConfigs(s3FileSystemConfiguration(storageLocationViews()))
                .build();
    }

    private static Object logfeederConfigStandaloneWlWithCloudStorageWithSolr() {
        return new TemplateModelContextBuilder()
                .withFileSystemConfigs(s3FileSystemConfiguration(storageLocationViews()))
                .withComponents(Set.of("INFRA_SOLR"))
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
        sharedServiceConfigsView.setRangerAdminPort("6080");
        sharedServiceConfigsView.setRangerAdminHost("1.1.1.1");
        return Optional.of(sharedServiceConfigsView);
    }
}
