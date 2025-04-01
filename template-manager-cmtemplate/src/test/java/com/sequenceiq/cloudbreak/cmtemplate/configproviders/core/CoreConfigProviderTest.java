package com.sequenceiq.cloudbreak.cmtemplate.configproviders.core;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.CORE_DEFAULTFS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.CORE_SETTINGS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.STORAGEOPERATIONS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles.HDFS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles.NAMENODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.adls.AdlsGen2ConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.s3.S3ConfigProvider;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
class CoreConfigProviderTest {

    private static final String TEST_USER_CRN = "crn:cdp:iam:us-west-1:accid:user:mockuser@cloudera.com";

    @InjectMocks
    private CoreConfigProvider underTest;

    @Mock
    private S3ConfigProvider s3ConfigProvider;

    @Mock
    private AdlsGen2ConfigProvider adlsGen2ConfigProvider;

    @Test
    void isConfigurationNeededWhenKafkaPresentedHdfsNotAndStorageConfiguredMustReturnTrue() {
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject templatePreparationObject = mock(TemplatePreparationObject.class);
        BaseFileSystemConfigurationsView fileSystemConfiguration = mock(BaseFileSystemConfigurationsView.class);
        GeneralClusterConfigs generalClusterConfigs = mock(GeneralClusterConfigs.class);

        List<StorageLocationView> storageLocationViews = new ArrayList<>();
        StorageLocation storageLocation = new StorageLocation();
        storageLocation.setConfigFile("core_defaultfs");
        storageLocation.setProperty("core_defaultfs");
        storageLocation.setValue("s3a://default-bucket/");
        storageLocationViews.add(new StorageLocationView(storageLocation));

        when(fileSystemConfiguration.getLocations()).thenReturn(storageLocationViews);
        Optional<BaseFileSystemConfigurationsView> fileSystemConfigurationView = Optional.of(fileSystemConfiguration);

        when(mockTemplateProcessor.isRoleTypePresentInService(HDFS, List.of(NAMENODE))).thenReturn(false);
        when(templatePreparationObject.getFileSystemConfigurationView()).thenReturn(fileSystemConfigurationView);
        when(templatePreparationObject.getGeneralClusterConfigs()).thenReturn(generalClusterConfigs);

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(mockTemplateProcessor, templatePreparationObject);
            Map<String, ApiClusterTemplateService> additionalServices = underTest.getAdditionalServices(mockTemplateProcessor, templatePreparationObject);
            List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(mockTemplateProcessor, templatePreparationObject);
            assertTrue(underTest.isConfigurationNeeded(mockTemplateProcessor, templatePreparationObject));
            assertEquals(0, roleConfigs.size());
            assertEquals(0, additionalServices.size());
            assertEquals(2, serviceConfigs.size());
        });
    }

    @Test
    void isConfigurationNeededWhenNotPresentedHdfsNotAndStorageConfiguredMustReturnTrue() {
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject templatePreparationObject = mock(TemplatePreparationObject.class);
        BaseFileSystemConfigurationsView fileSystemConfiguration = mock(BaseFileSystemConfigurationsView.class);

        List<StorageLocationView> storageLocationViews = new ArrayList<>();
        StorageLocation storageLocation = new StorageLocation();
        storageLocation.setConfigFile("core_defaultfs1");
        storageLocation.setProperty("core_defaultfs1");
        storageLocation.setValue("s3a://default-bucket/");
        storageLocationViews.add(new StorageLocationView(storageLocation));

        when(fileSystemConfiguration.getLocations()).thenReturn(storageLocationViews);
        Optional<BaseFileSystemConfigurationsView> fileSystemConfigurationView = Optional.of(fileSystemConfiguration);

        when(mockTemplateProcessor.isRoleTypePresentInService(HDFS, List.of(NAMENODE))).thenReturn(false);
        when(templatePreparationObject.getFileSystemConfigurationView()).thenReturn(fileSystemConfigurationView);

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            assertFalse(underTest.isConfigurationNeeded(mockTemplateProcessor, templatePreparationObject));
        });
    }

    @Test
    void isConfigurationNotNeededWhenNotPresentedHdfsNotAndStorageConfiguredAndDefaultFsNotConfiguredMustReturnFalse() {
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject templatePreparationObject = mock(TemplatePreparationObject.class);
        BaseFileSystemConfigurationsView fileSystemConfiguration = mock(BaseFileSystemConfigurationsView.class);
        Optional<BaseFileSystemConfigurationsView> fileSystemConfigurationView = Optional.of(fileSystemConfiguration);

        when(mockTemplateProcessor.isRoleTypePresentInService(HDFS, List.of(NAMENODE))).thenReturn(false);
        when(templatePreparationObject.getFileSystemConfigurationView()).thenReturn(fileSystemConfigurationView);

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            assertFalse(underTest.isConfigurationNeeded(mockTemplateProcessor, templatePreparationObject));
        });
    }

    @Test
    void isConfigurationNeededWhenKafkaPresentedHdfsPresentedAndStorageConfiguredMustReturnFalse() {
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject templatePreparationObject = mock(TemplatePreparationObject.class);
        GeneralClusterConfigs generalClusterConfigs = mock(GeneralClusterConfigs.class);
        when(mockTemplateProcessor.isRoleTypePresentInService(HDFS, List.of(NAMENODE))).thenReturn(true);
        when(templatePreparationObject.getGeneralClusterConfigs()).thenReturn(generalClusterConfigs);
        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(mockTemplateProcessor, templatePreparationObject);
            Map<String, ApiClusterTemplateService> additionalServices = underTest.getAdditionalServices(mockTemplateProcessor, templatePreparationObject);
            List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(mockTemplateProcessor, templatePreparationObject);
            assertFalse(underTest.isConfigurationNeeded(mockTemplateProcessor, templatePreparationObject));
        });
    }

    @Test
    void isConfigurationNeededWhenKafkaPresentedHdfsNotAndStorageNotConfiguredMustReturnFalse() {
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject templatePreparationObject = mock(TemplatePreparationObject.class);
        Optional<BaseFileSystemConfigurationsView> fileSystemConfigurationView = Optional.empty();
        GeneralClusterConfigs generalClusterConfigs = mock(GeneralClusterConfigs.class);

        when(mockTemplateProcessor.isRoleTypePresentInService(HDFS, List.of(NAMENODE))).thenReturn(false);
        when(templatePreparationObject.getFileSystemConfigurationView()).thenReturn(fileSystemConfigurationView);
        when(templatePreparationObject.getGeneralClusterConfigs()).thenReturn(generalClusterConfigs);

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(mockTemplateProcessor, templatePreparationObject);
            Map<String, ApiClusterTemplateService> additionalServices = underTest.getAdditionalServices(mockTemplateProcessor, templatePreparationObject);
            List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(mockTemplateProcessor, templatePreparationObject);
            assertFalse(underTest.isConfigurationNeeded(mockTemplateProcessor, templatePreparationObject));
        });
    }

    @Test
    void isHdfsSecurityGroupCacheReloadPropertyPresent() {
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject templatePreparationObject = mock(TemplatePreparationObject.class);
        BaseFileSystemConfigurationsView fileSystemConfiguration = mock(BaseFileSystemConfigurationsView.class);
        Optional<BaseFileSystemConfigurationsView> fileSystemConfigurationView = Optional.of(fileSystemConfiguration);
        StorageLocationView storageLocationView = mock(StorageLocationView.class);
        GeneralClusterConfigs generalClusterConfigs = mock(GeneralClusterConfigs.class);
        when(fileSystemConfigurationView.get().getLocations()).thenReturn(List.of(storageLocationView));
        when(storageLocationView.getProperty()).thenReturn("core_defaultfs");
        when(templatePreparationObject.getGeneralClusterConfigs()).thenReturn(generalClusterConfigs);

        when(mockTemplateProcessor.getRoleConfig(CORE_SETTINGS, STORAGEOPERATIONS, CORE_DEFAULTFS)).thenReturn(Optional.empty());
        when(mockTemplateProcessor.getStackVersion()).thenReturn("7.2.15");
        when(templatePreparationObject.getFileSystemConfigurationView()).thenReturn(fileSystemConfigurationView);
        doNothing().when(s3ConfigProvider).getServiceConfigs(any(TemplatePreparationObject.class), any(StringBuilder.class));
        doNothing().when(adlsGen2ConfigProvider).populateServiceConfigs(any(TemplatePreparationObject.class), any(StringBuilder.class), anyString());
        String coreSafetyValveProperty = ConfigUtils.getSafetyValveProperty("hadoop.security.groups.cache.background.reload", "true");
        List<ApiClusterTemplateConfig> expected = List.of(config("core_site_safety_valve", coreSafetyValveProperty),
                config("core_defaultfs", null));

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            assertThat(underTest.getServiceConfigs(mockTemplateProcessor, templatePreparationObject)).hasSameElementsAs(expected);
        });
    }

    @Test
    void testGetAdditionalServicesWhenConfigurationNeededAndCmVersionEarlierThan771() {
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject templatePreparationObject = mock(TemplatePreparationObject.class);
        BaseFileSystemConfigurationsView fileSystemConfiguration = mock(BaseFileSystemConfigurationsView.class);
        Optional<BaseFileSystemConfigurationsView> fileSystemConfigurationView = Optional.of(fileSystemConfiguration);
        StorageLocationView storageLocationView = mock(StorageLocationView.class);
        when(fileSystemConfiguration.getLocations()).thenReturn(List.of(storageLocationView));
        when(storageLocationView.getProperty()).thenReturn(CORE_DEFAULTFS);
        when(mockTemplateProcessor.getServiceByType(CORE_SETTINGS)).thenReturn(Optional.empty());
        when(templatePreparationObject.getFileSystemConfigurationView()).thenReturn(fileSystemConfigurationView);
        when(templatePreparationObject.getHostgroupViews()).thenReturn(Set.of(new HostgroupView("aGateway", 0, InstanceGroupType.GATEWAY, 1)));
        when(mockTemplateProcessor.getCmVersion()).thenReturn(Optional.of(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_6_0.getVersion()));

        Map<String, ApiClusterTemplateService> additionalServices = underTest.getAdditionalServices(mockTemplateProcessor, templatePreparationObject);

        assertFalse(additionalServices.isEmpty());
        assertThat(additionalServices).allSatisfy((group, serviceConfig) -> {
            assertThat(serviceConfig.getServiceType()).isEqualTo(CORE_SETTINGS);
            assertThat(serviceConfig.getRoleConfigGroups()).anyMatch(templateRole -> STORAGEOPERATIONS.equals(templateRole.getRoleType()));
        });
    }

    @Test
    void testGetAdditionalServicesWhenConfigurationNeededAndCmVersionNewerOrEqualsThan771() {
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject templatePreparationObject = mock(TemplatePreparationObject.class);
        BaseFileSystemConfigurationsView fileSystemConfiguration = mock(BaseFileSystemConfigurationsView.class);
        Optional<BaseFileSystemConfigurationsView> fileSystemConfigurationView = Optional.of(fileSystemConfiguration);
        StorageLocationView storageLocationView = mock(StorageLocationView.class);
        when(storageLocationView.getProperty()).thenReturn(CORE_DEFAULTFS);
        when(fileSystemConfiguration.getLocations()).thenReturn(List.of(storageLocationView));
        when(mockTemplateProcessor.getServiceByType(CORE_SETTINGS)).thenReturn(Optional.empty());
        when(mockTemplateProcessor.isRoleTypePresentInService(HDFS, List.of(NAMENODE))).thenReturn(false);
        when(templatePreparationObject.getFileSystemConfigurationView()).thenReturn(fileSystemConfigurationView);
        when(templatePreparationObject.getHostgroupViews()).thenReturn(Set.of(new HostgroupView("aGateway", 0, InstanceGroupType.GATEWAY, 1)));
        when(mockTemplateProcessor.getCmVersion()).thenReturn(Optional.of(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_7_1.getVersion()));

        assertTrue(underTest.isConfigurationNeeded(mockTemplateProcessor, templatePreparationObject));
        Map<String, ApiClusterTemplateService> additionalServices = underTest.getAdditionalServices(mockTemplateProcessor, templatePreparationObject);

        assertFalse(additionalServices.isEmpty());
        assertThat(additionalServices).allSatisfy((group, serviceConfig) -> {
            assertThat(serviceConfig.getServiceType()).isEqualTo(CORE_SETTINGS);
            assertThat(serviceConfig.getRoleConfigGroups()).noneMatch(templateRole -> STORAGEOPERATIONS.equals(templateRole.getRoleType()));
        });
    }

    @Test
    void testGetAdditionalServicesWhenConfigurationNeededAndCmVersionIsEmpty() {
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject templatePreparationObject = mock(TemplatePreparationObject.class);
        BaseFileSystemConfigurationsView fileSystemConfiguration = mock(BaseFileSystemConfigurationsView.class);
        Optional<BaseFileSystemConfigurationsView> fileSystemConfigurationView = Optional.of(fileSystemConfiguration);
        StorageLocationView storageLocationView = mock(StorageLocationView.class);
        when(storageLocationView.getProperty()).thenReturn(CORE_DEFAULTFS);
        when(fileSystemConfiguration.getLocations()).thenReturn(List.of(storageLocationView));
        when(mockTemplateProcessor.getServiceByType(CORE_SETTINGS)).thenReturn(Optional.empty());
        when(templatePreparationObject.getHostgroupViews()).thenReturn(Set.of(new HostgroupView("aGateway", 0, InstanceGroupType.GATEWAY, 1)));
        when(templatePreparationObject.getFileSystemConfigurationView()).thenReturn(fileSystemConfigurationView);
        when(mockTemplateProcessor.getCmVersion()).thenReturn(Optional.empty());

        Map<String, ApiClusterTemplateService> additionalServices = underTest.getAdditionalServices(mockTemplateProcessor, templatePreparationObject);

        assertFalse(additionalServices.isEmpty());
        assertThat(additionalServices).allSatisfy((group, serviceConfig) -> {
            assertThat(serviceConfig.getServiceType()).isEqualTo(CORE_SETTINGS);
            assertThat(serviceConfig.getRoleConfigGroups()).noneMatch(templateRole -> STORAGEOPERATIONS.equals(templateRole.getRoleType()));
        });
    }

    @Test
    void testHDFSSecurityConfigValueIsPrivacy() {
        HostgroupView gateway = new HostgroupView("gateway", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.CORE, 2);
        HostgroupView quorum = new HostgroupView("quorum", 0, InstanceGroupType.CORE, 3);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 3);

        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/namenode-ha.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withHostgroupViews(Set.of(gateway, master, quorum, worker))
                .withBlueprintView(new BlueprintView(inputJson, "CDP", "1.0", null, cmTemplateProcessor))
                .withStackType(StackType.DATALAKE)
                .build();

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs(CORE_SETTINGS, cmTemplateProcessor, preparationObject);
            assertFalse(roleConfigs.size() != 0);
        });

    }

    @Test
    void testGetServiceConfigsShouldContainHadoopRpcProtectionPrivacyIfGovCloud() {
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject templatePreparationObject = mock(TemplatePreparationObject.class);
        GeneralClusterConfigs generalClusterConfigs = mock(GeneralClusterConfigs.class);

        when(templatePreparationObject.getGeneralClusterConfigs()).thenReturn(generalClusterConfigs);
        when(generalClusterConfigs.isGovCloud()).thenReturn(true);

        String coreSafetyValveProperty = ConfigUtils.getSafetyValveProperty("hadoop.security.groups.cache.background.reload", "true");
        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(mockTemplateProcessor, templatePreparationObject);
            assertEquals(2, serviceConfigs.size());
            assertEquals("core_site_safety_valve", serviceConfigs.get(0).getName());
            assertEquals(coreSafetyValveProperty, serviceConfigs.get(0).getValue());
            assertEquals("hadoop_rpc_protection", serviceConfigs.get(1).getName());
            assertEquals("privacy", serviceConfigs.get(1).getValue());
        });
    }
}