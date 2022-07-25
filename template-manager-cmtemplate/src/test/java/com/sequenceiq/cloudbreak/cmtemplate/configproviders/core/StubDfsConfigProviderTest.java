package com.sequenceiq.cloudbreak.cmtemplate.configproviders.core;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.CORE_DEFAULTFS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.STORAGEOPERATIONS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.STUB_DFS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles.HDFS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles.NAMENODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
class StubDfsConfigProviderTest {

    @Mock
    private CmTemplateProcessor mockTemplateProcessor;

    @Mock
    private TemplatePreparationObject templatePreparationObject;

    @Mock
    private BaseFileSystemConfigurationsView fileSystemConfiguration;

    @Mock
    private StorageLocationView storageLocationView;

    @InjectMocks
    private StubDfsConfigProvider underTest;

    @Test
    void getAdditionalServicesWhenConfigurationNeedButStubDfsAlreadyConfigured() {
        ApiClusterTemplateService apiClusterTemplateService = mock(ApiClusterTemplateService.class);
        Optional<BaseFileSystemConfigurationsView> fileSystemConfigurationView = Optional.of(fileSystemConfiguration);
        when(storageLocationView.getProperty()).thenReturn(CORE_DEFAULTFS);
        when(fileSystemConfiguration.getLocations()).thenReturn(List.of(storageLocationView));
        when(mockTemplateProcessor.getServiceByType(STUB_DFS)).thenReturn(Optional.of(apiClusterTemplateService));
        when(mockTemplateProcessor.isRoleTypePresentInService(HDFS, List.of(NAMENODE))).thenReturn(false);
        when(mockTemplateProcessor.getCmVersion()).thenReturn(Optional.of(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_7_1.getVersion()));
        when(templatePreparationObject.getFileSystemConfigurationView()).thenReturn(fileSystemConfigurationView);

        Map<String, ApiClusterTemplateService> additionalServices = underTest.getAdditionalServices(mockTemplateProcessor, templatePreparationObject);

        assertTrue(additionalServices.isEmpty());
    }

    @Test
    void getAdditionalServicesWhenConfigurationNeedButStubDfsIsNotConfigured() {
        Optional<BaseFileSystemConfigurationsView> fileSystemConfigurationView = Optional.of(fileSystemConfiguration);
        when(storageLocationView.getProperty()).thenReturn(CORE_DEFAULTFS);
        when(fileSystemConfiguration.getLocations()).thenReturn(List.of(storageLocationView));
        when(mockTemplateProcessor.getServiceByType(STUB_DFS)).thenReturn(Optional.empty());
        when(mockTemplateProcessor.isRoleTypePresentInService(HDFS, List.of(NAMENODE))).thenReturn(false);
        when(templatePreparationObject.getFileSystemConfigurationView()).thenReturn(fileSystemConfigurationView);
        when(templatePreparationObject.getHostgroupViews()).thenReturn(Set.of(new HostgroupView("aGateway", 0, InstanceGroupType.GATEWAY, 1)));
        when(mockTemplateProcessor.getCmVersion()).thenReturn(Optional.of(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_7_1.getVersion()));

        Map<String, ApiClusterTemplateService> additionalServices = underTest.getAdditionalServices(mockTemplateProcessor, templatePreparationObject);

        assertFalse(additionalServices.isEmpty());
        assertThat(additionalServices).allSatisfy((group, serviceConfig) -> {
            assertThat(serviceConfig.getServiceType()).isEqualTo(STUB_DFS);
            assertThat(serviceConfig.getRoleConfigGroups()).allMatch(templateRole -> STORAGEOPERATIONS.equals(templateRole.getRoleType()));
        });
    }

    @Test
    void isConfigurationNeededWhenCmVersionIsNotSpecified() {
        when(mockTemplateProcessor.getCmVersion()).thenReturn(Optional.empty());

        assertFalse(underTest.isConfigurationNeeded(mockTemplateProcessor, templatePreparationObject));
    }

    @Test
    void isConfigurationNeededWhenCmVersionIsEarlierThan771() {
        when(mockTemplateProcessor.getCmVersion()).thenReturn(Optional.of(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_6.getVersion()));

        assertFalse(underTest.isConfigurationNeeded(mockTemplateProcessor, templatePreparationObject));
    }

    @Test
    void isConfigurationNeededWhenThereIsHdfsServiceWithNameNodeRole() {
        when(mockTemplateProcessor.getCmVersion()).thenReturn(Optional.of(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_7_1.getVersion()));
        when(mockTemplateProcessor.isRoleTypePresentInService(HDFS, List.of(NAMENODE))).thenReturn(true);

        assertFalse(underTest.isConfigurationNeeded(mockTemplateProcessor, templatePreparationObject));
    }

    @Test
    void isConfigurationNeededWhenNoFileSystemConfigurationProvided() {
        when(mockTemplateProcessor.getCmVersion()).thenReturn(Optional.of(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_7_1.getVersion()));
        when(mockTemplateProcessor.isRoleTypePresentInService(HDFS, List.of(NAMENODE))).thenReturn(false);
        when(templatePreparationObject.getFileSystemConfigurationView()).thenReturn(Optional.empty());


        assertFalse(underTest.isConfigurationNeeded(mockTemplateProcessor, templatePreparationObject));
    }

    @Test
    void isConfigurationNeededWhenNoCoreDefaultFsConfigured() {
        when(mockTemplateProcessor.getCmVersion()).thenReturn(Optional.of(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_7_1.getVersion()));
        when(mockTemplateProcessor.isRoleTypePresentInService(HDFS, List.of(NAMENODE))).thenReturn(false);
        when(templatePreparationObject.getFileSystemConfigurationView()).thenReturn(Optional.of(fileSystemConfiguration));
        when(fileSystemConfiguration.getLocations()).thenReturn(List.of(storageLocationView));
        when(storageLocationView.getProperty()).thenReturn("");

        assertFalse(underTest.isConfigurationNeeded(mockTemplateProcessor, templatePreparationObject));
    }

    @Test
    void isConfigurationNeededWhenCmVersionIsEqualsOrNewerThan771AnfFileSystemConfiguredWithoutHdfsAndNameNodeInTheTemplate() {
        when(mockTemplateProcessor.getCmVersion()).thenReturn(Optional.of(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_7_1.getVersion()));
        when(mockTemplateProcessor.isRoleTypePresentInService(HDFS, List.of(NAMENODE))).thenReturn(false);
        when(templatePreparationObject.getFileSystemConfigurationView()).thenReturn(Optional.of(fileSystemConfiguration));
        when(fileSystemConfiguration.getLocations()).thenReturn(List.of(storageLocationView));
        when(storageLocationView.getProperty()).thenReturn(CORE_DEFAULTFS);

        assertTrue(underTest.isConfigurationNeeded(mockTemplateProcessor, templatePreparationObject));
    }
}