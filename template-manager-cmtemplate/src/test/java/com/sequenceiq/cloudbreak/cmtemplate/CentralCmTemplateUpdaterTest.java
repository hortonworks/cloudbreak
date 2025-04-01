package com.sequenceiq.cloudbreak.cmtemplate;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.adls.AdlsGen2ConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.StubDfsConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase.HbaseCloudStorageServiceConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hue.HueConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.s3.S3ConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.inifile.IniFileFactory;
import com.sequenceiq.cloudbreak.domain.RdsSslMode;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplateProcessor;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.filesystem.TemplateCoreTestUtil;
import com.sequenceiq.cloudbreak.template.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.CustomConfigurationPropertyView;
import com.sequenceiq.cloudbreak.template.views.CustomConfigurationsView;
import com.sequenceiq.cloudbreak.template.views.GatewayView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.template.views.ProductDetailsView;
import com.sequenceiq.cloudbreak.template.views.RdsView;
import com.sequenceiq.cloudbreak.template.views.provider.RdsViewProvider;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.filesystem.S3FileSystem;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CentralCmTemplateUpdaterTest {

    private static final String TEST_USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String FQDN = "fqdn";

    private static final String SSL_CERTIFICATE_FILE_PATH = "/foo/bar.pem";

    @InjectMocks
    private CentralCmTemplateUpdater generator;

    @InjectMocks
    private HbaseCloudStorageServiceConfigProvider hbaseCloudStorageProvider;

    @InjectMocks
    private CoreConfigProvider coreConfigProvider;

    @InjectMocks
    private HueConfigProvider hueConfigProvider;

    @Spy
    private TemplateProcessor templateProcessor;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Spy
    private CmTemplateComponentConfigProviderProcessor cmTemplateComponentConfigProviderProcessor;

    @Spy
    private CmTemplateConfigInjectorProcessor cmTemplateConfigInjectorProcessor;

    @Spy
    private CustomConfigurationsInjectorProcessor customConfigurationsInjectorProcessor;

    @Spy
    private CmHostGroupRoleConfigProviderProcessor cmHostGroupRoleConfigProviderProcessor;

    @Mock
    private TemplatePreparationObject templatePreparationObject;

    @Mock
    private BlueprintView blueprintView;

    @Mock
    private BlueprintTextProcessor blueprintTextProcessor;

    @Mock
    private CustomConfigurationsView customConfigurationsView;

    @Mock
    private S3ConfigProvider s3ConfigProvider;

    @Mock
    private AdlsGen2ConfigProvider adlsConfigProvider;

    @Mock
    private GeneralClusterConfigs generalClusterConfigs;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private RdsViewProvider rdsViewProvider;

    @Spy
    private IniFileFactory iniFileFactory;

    private ClouderaManagerRepo clouderaManagerRepo;

    @BeforeEach
    public void setUp() {
        when(entitlementService.sdxHbaseCloudStorageEnabled(anyString())).thenReturn(true);

        List<CmTemplateComponentConfigProvider> cmTemplateComponentConfigProviders = List.of(
                new HiveMetastoreConfigProvider(),
                coreConfigProvider,
                hbaseCloudStorageProvider,
                new StubDfsConfigProvider(),
                hueConfigProvider);
        when(cmTemplateProcessorFactory.get(anyString())).thenAnswer(i -> new CmTemplateProcessor(i.getArgument(0)));
        when(templatePreparationObject.getBlueprintView()).thenReturn(blueprintView);
        when(blueprintView.getProcessor()).thenReturn(blueprintTextProcessor);
        when(templatePreparationObject.getHostgroupViews()).thenReturn(toHostgroupViews(getHostgroupMappings()));
        when(templatePreparationObject.getGeneralClusterConfigs()).thenReturn(generalClusterConfigs);
        doNothing().when(s3ConfigProvider).getServiceConfigs(any(TemplatePreparationObject.class), any(StringBuilder.class));
        doNothing().when(adlsConfigProvider).populateServiceConfigs(any(TemplatePreparationObject.class), any(StringBuilder.class), anyString());
        RdsConfigWithoutCluster rdsConfigHive = TestUtil.rdsConfigWithoutCluster(DatabaseType.HIVE, RdsSslMode.DISABLED);
        RdsView rdsViewHive = TemplateCoreTestUtil.rdsViewProvider().getRdsView(rdsConfigHive, "AWS", false);
        RdsConfigWithoutCluster rdsConfigHueSsl = TestUtil.rdsConfigWithoutCluster(DatabaseType.HUE, RdsSslMode.ENABLED);
        RdsView rdsViewHueSsl = TemplateCoreTestUtil.rdsViewProvider().getRdsView(rdsConfigHueSsl, SSL_CERTIFICATE_FILE_PATH, "AWS", true);
        when(templatePreparationObject.getRdsViews()).thenReturn(Set.of(rdsViewHive, rdsViewHueSsl));
        when(templatePreparationObject.getRdsView(DatabaseType.HIVE)).thenReturn(rdsViewHive);
        when(templatePreparationObject.getRdsView(DatabaseType.HUE)).thenReturn(rdsViewHueSsl);
        when(templatePreparationObject.getRdsSslCertificateFilePath()).thenReturn(SSL_CERTIFICATE_FILE_PATH);
        when(templatePreparationObject.getCustomConfigurationsView()).thenReturn(Optional.of(customConfigurationsView));
        when(templatePreparationObject.getStackType()).thenReturn(StackType.WORKLOAD);

        List<StorageLocationView> locations = new ArrayList<>();
        StorageLocation hbaseRootDir = new StorageLocation();
        hbaseRootDir.setProperty("hbase.rootdir");
        hbaseRootDir.setValue("s3a://bucket/cluster1/hbase");
        locations.add(new StorageLocationView(hbaseRootDir));
        S3FileSystemConfigurationsView fileSystemConfigurationsView =
                new S3FileSystemConfigurationsView(new S3FileSystem(), locations, false);
        when(templatePreparationObject.getFileSystemConfigurationView()).thenReturn(Optional.of(fileSystemConfigurationsView));
        when(generalClusterConfigs.getClusterName()).thenReturn("testcluster");
        when(generalClusterConfigs.getPassword()).thenReturn("Admin123!");
        when(generalClusterConfigs.getAccountId()).thenReturn(Optional.of("1234"));
        clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setVersion("6.1.0");
        ProductDetailsView productDetailsView = new ProductDetailsView(clouderaManagerRepo, List.of());
        when(templatePreparationObject.getProductDetailsView()).thenReturn(productDetailsView);
        ReflectionTestUtils.setField(cmTemplateComponentConfigProviderProcessor, "providers", cmTemplateComponentConfigProviders);
        ReflectionTestUtils.setField(cmTemplateConfigInjectorProcessor, "injectors", List.of());
        ReflectionTestUtils.setField(cmHostGroupRoleConfigProviderProcessor, "providers", List.of());
    }

    private static Set<HostgroupView> toHostgroupViews(Map<String, List<Map<String, String>>> hostgroupMappings) {
        return hostgroupMappings.entrySet().stream()
                .map(entry -> new HostgroupView(entry.getKey(), 0, InstanceGroupType.CORE,
                        entry.getValue().stream()
                                .map(each -> each.get(FQDN))
                                .collect(toSet())
                ))
                .collect(toSet());
    }

    @Test
    public void getCmTemplate() {
        when(blueprintView.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager.bp"));
        ApiClusterTemplate generated = testGetCmTemplate();
        assertMatchesBlueprintAtPath("output/clouderamanager.bp", generated);
    }

    @Test
    public void getCmTemplateWhenShouldNotSplitJNAndZK() {
        when(blueprintView.getBlueprintText()).thenReturn(getBlueprintText("input/cb5660.bp"));
        ApiClusterTemplate generated = testGetCmTemplate();
        assertMatchesBlueprintAtPath("output/cb5660.bp", generated);
    }

    @Test
    public void danglingVariableReferencesAreRemoved() {
        when(blueprintView.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-variables.bp"));
        ApiClusterTemplate generated = testGetCmTemplate();
        assertMatchesBlueprintAtPath("output/clouderamanager-variables.bp", generated);
    }

    @Test
    public void configsAreInjected() {
        List<ApiClusterTemplateConfig> serviceConfigs = List.of(config("service_config_name", "service_config_value"));
        List<ApiClusterTemplateConfig> roleConfigs = List.of(config("role_config_name", "role_config_value"));
        ReflectionTestUtils.setField(cmTemplateConfigInjectorProcessor, "injectors", List.of(new CmTemplateConfigInjector() {
            @Override
            public List<ApiClusterTemplateConfig> getServiceConfigs(ApiClusterTemplateService service, TemplatePreparationObject source) {
                return serviceConfigs;
            }

            @Override
            public List<ApiClusterTemplateConfig> getRoleConfigs(
                    ApiClusterTemplateRoleConfigGroup roleConfigGroup, ApiClusterTemplateService service, TemplatePreparationObject source) {
                return roleConfigs;
            }
        }));
        when(blueprintView.getBlueprintText()).thenReturn(getBlueprintText("input/namenode-ha.bp"));
        ApiClusterTemplate generated = testGetCmTemplate();
        assertMatchesBlueprintAtPath("output/namenode-ha-injected.bp", generated);
    }

    @Test
    public void customConfigsAreInjected() {
        when(customConfigurationsView.getConfigurations()).thenReturn(Set.of(
                new CustomConfigurationPropertyView("service_config_name", "service_config_value", null, "zookeeper"),
                new CustomConfigurationPropertyView("service_config_name", "service_config_value", null, "hdfs"),
                new CustomConfigurationPropertyView("role_config_name", "role_config_value", "server", "zookeeper"),
                new CustomConfigurationPropertyView("role_config_name", "role_config_value", "namenode", "hdfs"),
                new CustomConfigurationPropertyView("role_config_name", "role_config_value", "datanode", "hdfs"),
                new CustomConfigurationPropertyView("role_config_name", "role_config_value", "gateway", "hdfs"),
                new CustomConfigurationPropertyView("role_config_name", "role_config_value", "journalnode", "hdfs"),
                new CustomConfigurationPropertyView("role_config_name", "role_config_value", "failovercontroller", "hdfs"),
                new CustomConfigurationPropertyView("role_config_name", "role_config_value", "balancer", "hdfs")
        ));
        when(blueprintView.getBlueprintText()).thenReturn(getBlueprintText("input/namenode-ha.bp"));
        ApiClusterTemplate generated = testGetCmTemplate();
        assertMatchesBlueprintAtPath("output/namenode-ha-injected.bp", generated);
    }

    @Test
    public void getCmTemplateNoMetastore() {
        when(blueprintView.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-nometastore.bp"));
        ApiClusterTemplate generated = testGetCmTemplate();
        assertMatchesBlueprintAtPath("output/clouderamanager-nometastore.bp", generated);
    }

    @Test
    public void getCmTemplateIfCoreSettingsPresentShouldNotOverrideTheProperty() {
        List<StorageLocationView> locations = new ArrayList<>();

        StorageLocation hbaseRootDir = new StorageLocation();
        hbaseRootDir.setProperty("hbase.rootdir");
        hbaseRootDir.setValue("s3a://bucket/cluster1/hbase");
        locations.add(new StorageLocationView(hbaseRootDir));

        StorageLocation coreSettings = new StorageLocation();
        coreSettings.setProperty("core_defaultfs");
        coreSettings.setValue("s3a://bucket/cluster1/hbase");
        locations.add(new StorageLocationView(coreSettings));

        S3FileSystemConfigurationsView fileSystemConfigurationsView =
                new S3FileSystemConfigurationsView(new S3FileSystem(), locations, false);
        when(templatePreparationObject.getFileSystemConfigurationView()).thenReturn(Optional.of(fileSystemConfigurationsView));

        when(blueprintView.getBlueprintText()).thenReturn(getBlueprintText("input/core-settings.bp"));
        ApiClusterTemplate generated = testGetCmTemplate();
        assertMatchesBlueprintAtPath("output/core-settings.bp", generated);
    }

    @Test
    public void getCmTemplateIfCoreSettingsPresentShouldNotOverrideThePropertyWithEmptyString() {
        List<StorageLocationView> locations = new ArrayList<>();

        StorageLocation hbaseRootDir = new StorageLocation();
        hbaseRootDir.setProperty("hbase.rootdir");
        hbaseRootDir.setValue("s3a://bucket/cluster1/hbase");
        locations.add(new StorageLocationView(hbaseRootDir));

        StorageLocation coreSettings = new StorageLocation();
        coreSettings.setProperty("core_defaultfs");
        coreSettings.setValue("s3a://bucket/cluster1/hbase");
        locations.add(new StorageLocationView(coreSettings));

        S3FileSystemConfigurationsView fileSystemConfigurationsView =
                new S3FileSystemConfigurationsView(new S3FileSystem(), locations, false);
        when(templatePreparationObject.getFileSystemConfigurationView()).thenReturn(Optional.of(fileSystemConfigurationsView));

        when(blueprintView.getBlueprintText()).thenReturn(getBlueprintText("input/core-settings-empty.bp"));
        ApiClusterTemplate generated = testGetCmTemplate();
        assertMatchesBlueprintAtPath("output/core-settings-empty.bp", generated);
    }

    @Test
    public void getCmTemplateNoMetastoreWithTemplateParams() {
        when(blueprintView.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-fixparam.bp"));
        ApiClusterTemplate generated = testGetCmTemplate();
        assertMatchesBlueprintAtPath("output/clouderamanager-fixparam.bp", generated);
    }

    private ApiClusterTemplate testGetCmTemplate() {
        return ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN,
                () -> generator.getCmTemplate(templatePreparationObject, getHostgroupMappings(), clouderaManagerRepo, null, null));
    }

    @Test
    public void getCmTemplateWithoutHosts() {
        when(blueprintView.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-without-hosts.bp"));
        String generated = ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> generator.getBlueprintText(templatePreparationObject));
        assertEquals(new CmTemplateProcessor(getBlueprintText("output/clouderamanager-without-hosts.bp")).getTemplate().toString(),
                new CmTemplateProcessor(generated).getTemplate().toString());
    }

    @Test
    public void getKafkaPropertiesWhenNoHdfsInClusterShouldPresentCoreSettings() {
        S3FileSystem s3FileSystem = new S3FileSystem();
        s3FileSystem.setInstanceProfile("profile");
        s3FileSystem.setS3GuardDynamoTableName("cb-table");
        s3FileSystem.setStorageContainer("cloudbreak-bucket");

        StorageLocation storageLocation = new StorageLocation();
        storageLocation.setProperty("core_defaultfs");
        storageLocation.setValue("s3a://cloudbreak-bucket/kafka");
        storageLocation.setConfigFile("core_settings");
        StorageLocationView storageLocationView = new StorageLocationView(storageLocation);

        BaseFileSystemConfigurationsView baseFileSystemConfigurationsView =
                new S3FileSystemConfigurationsView(s3FileSystem, Sets.newHashSet(storageLocationView), false);
        when(templatePreparationObject.getFileSystemConfigurationView()).thenReturn(Optional.of(baseFileSystemConfigurationsView));
        when(templatePreparationObject.getGatewayView()).thenReturn(new GatewayView(new Gateway(), "signkey", new HashSet<>()));
        Set<HostgroupView> hostgroupViews = new HashSet<>();
        hostgroupViews.add(new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1));
        when(templatePreparationObject.getHostgroupViews()).thenReturn(hostgroupViews);
        when(templatePreparationObject.getGatewayView()).thenReturn(new GatewayView(new Gateway(), "signkey", new HashSet<>()));

        when(blueprintView.getBlueprintText()).thenReturn(getBlueprintText("input/kafka-without-hdfs.bp"));
        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            String generated = generator.getBlueprintText(templatePreparationObject);
            String expected = new CmTemplateProcessor(getBlueprintText("output/kafka-without-hdfs.bp")).getTemplate().toString();
            String output = new CmTemplateProcessor(generated).getTemplate().toString();
            assertEquals(expected, output);
        });
    }

    @Test
    public void getKafkaPropertiesWhenNoHdfsInClusterAndCmNewerOrEqualsThan771ShouldPresentCoreSettingsAndStubDfsService() {
        S3FileSystem s3FileSystem = new S3FileSystem();
        s3FileSystem.setInstanceProfile("profile");
        s3FileSystem.setS3GuardDynamoTableName("cb-table");
        s3FileSystem.setStorageContainer("cloudbreak-bucket");

        StorageLocation storageLocation = new StorageLocation();
        storageLocation.setProperty("core_defaultfs");
        storageLocation.setValue("s3a://cloudbreak-bucket/kafka");
        storageLocation.setConfigFile("core_settings");
        StorageLocationView storageLocationView = new StorageLocationView(storageLocation);

        BaseFileSystemConfigurationsView baseFileSystemConfigurationsView =
                new S3FileSystemConfigurationsView(s3FileSystem, Sets.newHashSet(storageLocationView), false);
        when(templatePreparationObject.getFileSystemConfigurationView()).thenReturn(Optional.of(baseFileSystemConfigurationsView));
        when(templatePreparationObject.getGatewayView()).thenReturn(new GatewayView(new Gateway(), "signkey", new HashSet<>()));
        Set<HostgroupView> hostgroupViews = new HashSet<>();
        hostgroupViews.add(new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1));
        when(templatePreparationObject.getHostgroupViews()).thenReturn(hostgroupViews);
        when(templatePreparationObject.getGatewayView()).thenReturn(new GatewayView(new Gateway(), "signkey", new HashSet<>()));

        when(blueprintView.getBlueprintText()).thenReturn(getBlueprintText("input/kafka-without-hdfs-cm-771.bp"));
        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            String generated = generator.getBlueprintText(templatePreparationObject);
            String expected = new CmTemplateProcessor(getBlueprintText("output/kafka-without-hdfs-cm-771.bp")).getTemplate().toString();
            String output = new CmTemplateProcessor(generated).getTemplate().toString();
            assertEquals(expected, output);
        });
    }

    @Test
    public void getCmTemplateHueDbSsl() {
        when(blueprintView.getBlueprintText()).thenReturn(getBlueprintText("input/hue-ssl.bp"));
        when(blueprintTextProcessor.getVersion()).thenReturn(Optional.of("7.2.11"));
        clouderaManagerRepo.setVersion("7.4.3");
        ApiClusterTemplate generated = testGetCmTemplate();
        assertMatchesBlueprintAtPath("output/hue-ssl.bp", generated);
    }

    private void assertMatchesBlueprintAtPath(String path, ApiClusterTemplate generated) {
        assertEquals(String.valueOf(new CmTemplateProcessor(getBlueprintText(path)).getTemplate()), String.valueOf(generated));
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }

    private Map<String, List<Map<String, String>>> getHostgroupMappings() {
        Map<String, List<Map<String, String>>> result = new HashMap<>();
        List<Map<String, String>> hosts12 = new ArrayList<>();
        hosts12.add(Map.of(FQDN, "host1"));
        hosts12.add(Map.of(FQDN, "host2"));
        result.put("master", hosts12);
        List<Map<String, String>> hosts34 = new ArrayList<>();
        hosts34.add(Map.of(FQDN, "host3"));
        hosts34.add(Map.of(FQDN, "host4"));
        result.put("worker", hosts34);
        return result;
    }

}