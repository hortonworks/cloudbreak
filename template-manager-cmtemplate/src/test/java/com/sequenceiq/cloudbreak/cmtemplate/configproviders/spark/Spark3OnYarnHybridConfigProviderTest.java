package com.sequenceiq.cloudbreak.cmtemplate.configproviders.spark;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.spark.Spark3OnYarnHybridConfigProvider.SPARK3_HISTORY_LOG_DIR;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.spark.Spark3OnYarnHybridConfigProvider.SPARK3_HISTORY_PATH;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.spark.Spark3OnYarnHybridConfigProvider.SPARK3_KERBEROS_FILESYSTEMS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsConfigHelper;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.common.model.CloudStorageCdpService;

@ExtendWith(MockitoExtension.class)
class Spark3OnYarnHybridConfigProviderTest {

    @Mock
    private HdfsConfigHelper hdfsConfigHelper;

    @InjectMocks
    private Spark3OnYarnHybridConfigProvider underTest;

    @Mock
    private CmTemplateProcessor templateProcessor;

    @Mock
    private TemplatePreparationObject source;

    @Mock
    private BaseFileSystemConfigurationsView fileSystemConfigurationView;

    @Mock
    private StorageLocationView storageLocationView;

    @BeforeEach
    void setUp() {
        lenient().when(source.getFileSystemConfigurationView()).thenReturn(Optional.of(fileSystemConfigurationView));
        lenient().when(fileSystemConfigurationView.getLocations()).thenReturn(List.of(storageLocationView));
    }

    @Test
    void getServiceConfigs() {
        String dhHdfs = "hdfs://dh-hdfs";
        when(hdfsConfigHelper.getHdfsUrl(templateProcessor, source)).thenReturn(Optional.of(dhHdfs));

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(templateProcessor, source);

        assertThat(result).containsExactly(config(SPARK3_HISTORY_LOG_DIR, dhHdfs + SPARK3_HISTORY_PATH));
    }

    @Test
    void getRoleConfigs() {
        String dlHdfs = "hdfs://dl-hdfs";
        when(storageLocationView.getProperty()).thenReturn(CloudStorageCdpService.REMOTE_FS.name());
        when(storageLocationView.getValue()).thenReturn(dlHdfs);

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfigs(SparkRoles.GATEWAY, templateProcessor, source);

        assertThat(result).containsExactly(config(SPARK3_KERBEROS_FILESYSTEMS_CONFIG, dlHdfs));
    }

    @Test
    void isConfigurationNeededWithoutRole() {
        when(templateProcessor.isRoleTypePresentInService(any(), any())).thenReturn(false);
        boolean result = underTest.isConfigurationNeeded(templateProcessor, source);
        assertThat(result).isFalse();
    }

    @Test
    void isConfigurationNeededWithRoleForNonHybridDatahub() {
        when(templateProcessor.isRoleTypePresentInService(any(), any())).thenReturn(true);
        when(templateProcessor.isHybridDatahub(source)).thenReturn(false);
        boolean result = underTest.isConfigurationNeeded(templateProcessor, source);
        assertThat(result).isFalse();
    }

    @Test
    void isConfigurationNeededWithRoleForHybridDatahub() {
        when(templateProcessor.isRoleTypePresentInService(any(), any())).thenReturn(true);
        when(templateProcessor.isHybridDatahub(source)).thenReturn(true);
        boolean result = underTest.isConfigurationNeeded(templateProcessor, source);
        assertThat(result).isTrue();
    }

}
