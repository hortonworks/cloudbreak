package com.sequenceiq.cloudbreak.cmtemplate;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static java.util.stream.Collectors.toSet;
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

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase.HbaseCloudStorageServiceConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.s3.S3ConfigProvider;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplateProcessor;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.GatewayView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.template.views.ProductDetailsView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.filesystem.S3FileSystem;
import com.sequenceiq.common.api.type.InstanceGroupType;

@RunWith(MockitoJUnitRunner.class)
public class CentralCmTemplateUpdaterTest {

    private static final String FQDN = "fqdn";

    @InjectMocks
    private CentralCmTemplateUpdater generator = new CentralCmTemplateUpdater();

    @InjectMocks
    private HbaseCloudStorageServiceConfigProvider hbaseCloudStorageProvider = new HbaseCloudStorageServiceConfigProvider();

    @Spy
    private TemplateProcessor templateProcessor;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Spy
    private CmTemplateComponentConfigProviderProcessor cmTemplateComponentConfigProviderProcessor;

    @Spy
    private CmTemplateConfigInjectorProcessor cmTemplateConfigInjectorProcessor;

    @Spy
    private CmHostGroupRoleConfigProviderProcessor cmHostGroupRoleConfigProviderProcessor;

    @Mock
    private TemplatePreparationObject templatePreparationObject;

    @Mock
    private BlueprintView blueprintView;

    @Mock
    private S3ConfigProvider s3ConfigProvider;

    @Mock
    private GeneralClusterConfigs generalClusterConfigs;

    @Mock
    private EntitlementService entitlementService;

    private ClouderaManagerRepo clouderaManagerRepo;

    @Before
    public void setUp() {
        if (StringUtils.isEmpty(ThreadBasedUserCrnProvider.getUserCrn())) {
            ThreadBasedUserCrnProvider.setUserCrn("crn:cdp:iam:us-west-1:1234:user:1");
        }
        when(entitlementService.sdxHbaseCloudStorageEnabled(anyString())).thenReturn(true);

        List<CmTemplateComponentConfigProvider> cmTemplateComponentConfigProviders = List.of(new HiveMetastoreConfigProvider(),
                hbaseCloudStorageProvider);
        when(cmTemplateProcessorFactory.get(anyString())).thenAnswer(i -> new CmTemplateProcessor(i.getArgument(0)));
        when(templatePreparationObject.getBlueprintView()).thenReturn(blueprintView);
        when(templatePreparationObject.getHostgroupViews()).thenReturn(toHostgroupViews(getHostgroupMappings()));
        when(templatePreparationObject.getGeneralClusterConfigs()).thenReturn(generalClusterConfigs);
        doNothing().when(s3ConfigProvider).getServiceConfigs(any(TemplatePreparationObject.class), any(StringBuilder.class));
        RDSConfig rdsConfig = TestUtil.rdsConfig(DatabaseType.HIVE);
        when(templatePreparationObject.getRdsConfigs()).thenReturn(Set.of(rdsConfig));
        when(templatePreparationObject.getRdsConfig(DatabaseType.HIVE)).thenReturn(rdsConfig);

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
        ApiClusterTemplate generated = generator.getCmTemplate(templatePreparationObject, getHostgroupMappings(), clouderaManagerRepo, null, null);
        assertMatchesBlueprintAtPath("output/clouderamanager.bp", generated);
    }

    @Test
    public void getCmTemplateWhenShouldNotSplitJNAndZK() {
        when(blueprintView.getBlueprintText()).thenReturn(getBlueprintText("input/cb5660.bp"));
        ApiClusterTemplate generated = generator.getCmTemplate(templatePreparationObject, getHostgroupMappings(), clouderaManagerRepo, null, null);
        assertMatchesBlueprintAtPath("output/cb5660.bp", generated);
    }

    @Test
    public void danglingVariableReferencesAreRemoved() {
        when(blueprintView.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-variables.bp"));
        ApiClusterTemplate generated = generator.getCmTemplate(templatePreparationObject, getHostgroupMappings(), clouderaManagerRepo, null, null);
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
        ApiClusterTemplate generated = generator.getCmTemplate(templatePreparationObject, getHostgroupMappings(), clouderaManagerRepo, null, null);
        assertMatchesBlueprintAtPath("output/namenode-ha-injected.bp", generated);
    }

    @Test
    public void getCmTemplateNoMetastore() {
        when(blueprintView.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-nometastore.bp"));
        ApiClusterTemplate generated = generator.getCmTemplate(templatePreparationObject, getHostgroupMappings(), clouderaManagerRepo, null, null);
        assertMatchesBlueprintAtPath("output/clouderamanager-nometastore.bp", generated);
    }

    @Test
    public void getCmTemplateNoMetastoreWithTemplateParams() {
        when(blueprintView.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-fixparam.bp"));
        ApiClusterTemplate generated = generator.getCmTemplate(templatePreparationObject, getHostgroupMappings(), clouderaManagerRepo, null, null);
        assertMatchesBlueprintAtPath("output/clouderamanager-fixparam.bp", generated);
    }

    @Test
    public void getCmTemplateWithoutHosts() {
        when(blueprintView.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-without-hosts.bp"));
        String generated = generator.getBlueprintText(templatePreparationObject);
        Assert.assertEquals(new CmTemplateProcessor(getBlueprintText("output/clouderamanager-without-hosts.bp")).getTemplate().toString(),
                new CmTemplateProcessor(generated).getTemplate().toString());
    }

    @Test
    public void getKafkaPropertiesWhenNoHdfsInClusterShouldPresentCoreSettings() {
        CoreConfigProvider coreConfigProvider = new CoreConfigProvider();
        ReflectionTestUtils.setField(coreConfigProvider, "s3ConfigProvider", s3ConfigProvider);
        List<CmTemplateComponentConfigProvider> cmTemplateComponentConfigProviders = List.of(coreConfigProvider);
        ReflectionTestUtils.setField(cmTemplateComponentConfigProviderProcessor, "providers", cmTemplateComponentConfigProviders);
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
        String generated = generator.getBlueprintText(templatePreparationObject);
        String expected = new CmTemplateProcessor(getBlueprintText("output/kafka-without-hdfs.bp")).getTemplate().toString();
        String output = new CmTemplateProcessor(generated).getTemplate().toString();
        Assert.assertEquals(expected, output);
    }

    private void assertMatchesBlueprintAtPath(String path, ApiClusterTemplate generated) {
        Assert.assertEquals(
                String.valueOf(new CmTemplateProcessor(getBlueprintText(path)).getTemplate()),
                String.valueOf(generated)
        );
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