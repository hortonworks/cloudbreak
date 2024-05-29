package com.sequenceiq.cloudbreak.cmtemplate.configproviders.iceberg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class IcebergReplicationCloudStorageServiceConfigProviderTest {

    public static final String ICEBERG_REPLICATION_CLOUD_DATA_ROOT_DIR = "iceberg_replication_cloud_data_root_dir";

    public static final String ICEBERG_REPLICATION_CLOUD_DATA_ROOT = "iceberg_replication_cloud_data_root";

    public static final String ICEBERG_REPLICATION_CLOUD_DATA_ROOT_VALUE = "storageBaseLocation";

    @Mock
    CmTemplateProcessor cmTemplateProcessor;

    @Mock
    TemplatePreparationObject source;

    @Mock
    BaseFileSystemConfigurationsView baseFileSystemConfigurationsView;

    @Mock
    StorageLocationView storageLocationView;

    private final IcebergReplicationCloudStorageServiceConfigProvider underTest = new IcebergReplicationCloudStorageServiceConfigProvider();

    @BeforeEach
    void setUp() {
        when(storageLocationView.getProperty()).thenReturn(ICEBERG_REPLICATION_CLOUD_DATA_ROOT_DIR);
        when(storageLocationView.getValue()).thenReturn(ICEBERG_REPLICATION_CLOUD_DATA_ROOT_VALUE);
        when(baseFileSystemConfigurationsView.getLocations()).thenReturn(List.of(storageLocationView));
        when(source.getFileSystemConfigurationView()).thenReturn(Optional.of(baseFileSystemConfigurationsView));
        when(cmTemplateProcessor.isRoleTypePresentInService(IcebergRoles.ICEBERG_REPLICATION,
                List.of(IcebergRoles.ICEBERG_REPLICATION_ADMINSERVER))).thenReturn(true);
    }

    @Test
    void testGetServiceType() {
        assertEquals(IcebergRoles.ICEBERG_REPLICATION, underTest.getServiceType());
    }

    @Test
    void testGetRoleTypes() {
        assertEquals(List.of(IcebergRoles.ICEBERG_REPLICATION_ADMINSERVER), underTest.getRoleTypes());
    }

    @Test
    void testIsConfigurationNeededReturnsTrue() {
        boolean result = underTest.isConfigurationNeeded(cmTemplateProcessor, source);
        assertEquals(true, result);
        verify(cmTemplateProcessor).isRoleTypePresentInService(IcebergRoles.ICEBERG_REPLICATION, List.of(IcebergRoles.ICEBERG_REPLICATION_ADMINSERVER));
        verify(source).getFileSystemConfigurationView();
    }

    @Test
    void testIsConfigurationNeededFileSystemNotPresentReturnsFalse() {
        when(source.getFileSystemConfigurationView()).thenReturn(Optional.empty());
        boolean result = underTest.isConfigurationNeeded(cmTemplateProcessor, source);
        assertEquals(false, result);
        verify(cmTemplateProcessor, never()).isRoleTypePresentInService(IcebergRoles.ICEBERG_REPLICATION, List.of(IcebergRoles.ICEBERG_REPLICATION_ADMINSERVER));
        verify(source).getFileSystemConfigurationView();
    }

    @Test
    void testIsConfigurationNeededServiceNotPresentReturnsFalse() {
        when(cmTemplateProcessor.isRoleTypePresentInService(IcebergRoles.ICEBERG_REPLICATION,
                List.of(IcebergRoles.ICEBERG_REPLICATION_ADMINSERVER))).thenReturn(false);
        boolean result = underTest.isConfigurationNeeded(cmTemplateProcessor, source);
        assertEquals(false, result);
        verify(cmTemplateProcessor).isRoleTypePresentInService(IcebergRoles.ICEBERG_REPLICATION, List.of(IcebergRoles.ICEBERG_REPLICATION_ADMINSERVER));
        verify(source).getFileSystemConfigurationView();
    }

    @Test
    void testGetServiceConfigs() {
        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, source);
        assertEquals(1, serviceConfigs.size());
        assertEquals(ICEBERG_REPLICATION_CLOUD_DATA_ROOT, serviceConfigs.get(0).getName());
        assertEquals(ICEBERG_REPLICATION_CLOUD_DATA_ROOT_VALUE, serviceConfigs.get(0).getValue());
        verify(source).getFileSystemConfigurationView();
        verify(baseFileSystemConfigurationsView).getLocations();
        verify(storageLocationView).getProperty();
        verify(storageLocationView).getValue();
    }

    @Test
    void testGetServiceConfigsFileSystemNotPresent() {
        when(source.getFileSystemConfigurationView()).thenReturn(Optional.empty());
        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, source);
        assertEquals(0, serviceConfigs.size());
        verify(source).getFileSystemConfigurationView();
        verify(baseFileSystemConfigurationsView, never()).getLocations();
        verify(storageLocationView, never()).getProperty();
        verify(storageLocationView, never()).getValue();
    }

    @Test
    void testGetServiceConfigsPropertyNotPresent() {
        when(storageLocationView.getProperty()).thenReturn("test");
        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, source);
        assertEquals(0, serviceConfigs.size());
        verify(source).getFileSystemConfigurationView();
        verify(baseFileSystemConfigurationsView).getLocations();
        verify(storageLocationView).getProperty();
        verify(storageLocationView, never()).getValue();
    }
}
