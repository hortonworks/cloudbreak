package com.sequenceiq.cloudbreak.cmtemplate.configproviders.core;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles.HDFS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles.NAMENODE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaRoles.KAFKA_BROKER;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaRoles.KAFKA_SERVICE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;

public class CoreConfigProviderTest {

    private CoreConfigProvider underTest;

    @Before
    public void setUp() {
        underTest = new CoreConfigProvider();
    }

    @Test
    public void isConfigurationNeededWhenKafkaPresentedHdfsNotAndStorageConfiguredMustReturnTrue() {
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject templatePreparationObject = mock(TemplatePreparationObject.class);
        BaseFileSystemConfigurationsView fileSystemConfiguration = mock(BaseFileSystemConfigurationsView.class);
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
        Optional<BaseFileSystemConfigurationsView> fileSystemConfigurationView = Optional.of(fileSystemConfiguration);

        when(mockTemplateProcessor.isRoleTypePresentInService(HDFS, List.of(NAMENODE))).thenReturn(false);
        when(templatePreparationObject.getFileSystemConfigurationView()).thenReturn(fileSystemConfigurationView);

        Assert.assertTrue(underTest.isConfigurationNeeded(mockTemplateProcessor, templatePreparationObject));
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
}