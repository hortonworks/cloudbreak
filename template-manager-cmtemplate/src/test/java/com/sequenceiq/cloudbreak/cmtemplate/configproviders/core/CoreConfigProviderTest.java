package com.sequenceiq.cloudbreak.cmtemplate.configproviders.core;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles.HDFS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles.NAMENODE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaRoles.KAFKA_BROKER;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaRoles.KAFKA_SERVICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.s3.S3ConfigProvider;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;

@ExtendWith(MockitoExtension.class)
public class CoreConfigProviderTest {

    @InjectMocks
    private CoreConfigProvider underTest;

    @Mock
    private S3ConfigProvider s3ConfigProvider;

    @Before
    public void setUp() {
        underTest = new CoreConfigProvider();
        s3ConfigProvider = new S3ConfigProvider();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void isConfigurationNeededWhenKafkaPresentedHdfsNotAndStorageConfiguredMustReturnTrue() {
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject templatePreparationObject = mock(TemplatePreparationObject.class);
        BaseFileSystemConfigurationsView fileSystemConfiguration = mock(BaseFileSystemConfigurationsView.class);

        List<StorageLocationView> storageLocationViews = new ArrayList<>();
        StorageLocation storageLocation = new StorageLocation();
        storageLocation.setConfigFile("core_defaultfs");
        storageLocation.setProperty("core_defaultfs");
        storageLocation.setValue("s3a://default-bucket/");
        storageLocationViews.add(new StorageLocationView(storageLocation));

        when(fileSystemConfiguration.getLocations()).thenReturn(storageLocationViews);
        Optional<BaseFileSystemConfigurationsView> fileSystemConfigurationView = Optional.of(fileSystemConfiguration);

        when(mockTemplateProcessor.isRoleTypePresentInService(KAFKA_SERVICE, List.of(KAFKA_BROKER))).thenReturn(true);
        when(mockTemplateProcessor.isRoleTypePresentInService(HDFS, List.of(NAMENODE))).thenReturn(false);
        when(templatePreparationObject.getFileSystemConfigurationView()).thenReturn(fileSystemConfigurationView);

        Assert.assertTrue(underTest.isConfigurationNeeded(mockTemplateProcessor, templatePreparationObject));
    }

    @Test
    public void isConfigurationNeededWhenNotPresentedHdfsNotAndStorageConfiguredMustReturnTrue() {
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

        Assert.assertFalse(underTest.isConfigurationNeeded(mockTemplateProcessor, templatePreparationObject));
    }

    @Test
    public void isConfigurationNotNeededWhenNotPresentedHdfsNotAndStorageConfiguredAndDefaultFsNotConfiguredMustReturnFalse() {
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject templatePreparationObject = mock(TemplatePreparationObject.class);
        BaseFileSystemConfigurationsView fileSystemConfiguration = mock(BaseFileSystemConfigurationsView.class);
        Optional<BaseFileSystemConfigurationsView> fileSystemConfigurationView = Optional.of(fileSystemConfiguration);

        when(mockTemplateProcessor.isRoleTypePresentInService(HDFS, List.of(NAMENODE))).thenReturn(false);
        when(templatePreparationObject.getFileSystemConfigurationView()).thenReturn(fileSystemConfigurationView);

        Assert.assertFalse(underTest.isConfigurationNeeded(mockTemplateProcessor, templatePreparationObject));
    }

    @Test
    public void isConfigurationNeededWhenKafkaPresentedHdfsPresentedAndStorageConfiguredMustReturnFalse() {
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject templatePreparationObject = mock(TemplatePreparationObject.class);
        BaseFileSystemConfigurationsView fileSystemConfiguration = mock(BaseFileSystemConfigurationsView.class);
        Optional<BaseFileSystemConfigurationsView> fileSystemConfigurationView = Optional.of(fileSystemConfiguration);

        when(mockTemplateProcessor.isRoleTypePresentInService(KAFKA_SERVICE, List.of(KAFKA_BROKER))).thenReturn(true);
        when(mockTemplateProcessor.isRoleTypePresentInService(HDFS, List.of(NAMENODE))).thenReturn(true);
        when(templatePreparationObject.getFileSystemConfigurationView()).thenReturn(fileSystemConfigurationView);

        Assert.assertFalse(underTest.isConfigurationNeeded(mockTemplateProcessor, templatePreparationObject));
    }

    @Test
    public void isConfigurationNeededWhenKafkaPresentedHdfsNotAndStorageNotConfiguredMustReturnFalse() {
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject templatePreparationObject = mock(TemplatePreparationObject.class);
        Optional<BaseFileSystemConfigurationsView> fileSystemConfigurationView = Optional.empty();

        when(mockTemplateProcessor.isRoleTypePresentInService(KAFKA_SERVICE, List.of(KAFKA_BROKER))).thenReturn(true);
        when(mockTemplateProcessor.isRoleTypePresentInService(HDFS, List.of(NAMENODE))).thenReturn(false);
        when(templatePreparationObject.getFileSystemConfigurationView()).thenReturn(fileSystemConfigurationView);

        Assert.assertFalse(underTest.isConfigurationNeeded(mockTemplateProcessor, templatePreparationObject));
    }

    @Test
    public void isHdfsSecurityGroupCacheReloadPropertyPresent() {
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject templatePreparationObject = mock(TemplatePreparationObject.class);
        BaseFileSystemConfigurationsView fileSystemConfiguration = mock(BaseFileSystemConfigurationsView.class);
        Optional<BaseFileSystemConfigurationsView> fileSystemConfigurationView = Optional.of(fileSystemConfiguration);

        when(mockTemplateProcessor.isRoleTypePresentInService(KAFKA_SERVICE, List.of(KAFKA_BROKER))).thenReturn(true);
        when(mockTemplateProcessor.isRoleTypePresentInService(HDFS, List.of(NAMENODE))).thenReturn(true);
        when(templatePreparationObject.getFileSystemConfigurationView()).thenReturn(fileSystemConfigurationView);
        doNothing().when(s3ConfigProvider).getServiceConfigs(any(TemplatePreparationObject.class), any(StringBuilder.class));
        String coreSafetyValveProperty = ConfigUtils.getSafetyValveProperty("hadoop.security.groups.cache.background.reload", "true");
        List<ApiClusterTemplateConfig> expected = List.of(config("core_site_safety_valve", coreSafetyValveProperty));

        assertThat(underTest.getServiceConfigs(null, templatePreparationObject)).hasSameElementsAs(expected);
    }
}