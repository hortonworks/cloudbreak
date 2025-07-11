package com.sequenceiq.cloudbreak.cmtemplate.configproviders.tez;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.tez.TezHybridConfigProvider.TEZ_HISTORY_LOGGING_PROTO_BASE_DIR_KEY;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.tez.TezHybridConfigProvider.TEZ_HISTORY_LOGGING_PROTO_BASE_DIR_VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

@ExtendWith(MockitoExtension.class)
class TezHybridConfigProviderTest {

    @Mock
    private HdfsConfigHelper hdfsConfigHelper;

    @InjectMocks
    private TezHybridConfigProvider underTest;

    @Mock
    private CmTemplateProcessor templateProcessor;

    @Mock
    private TemplatePreparationObject source;

    @BeforeEach
    void setUp() {
    }

    @Test
    void getServiceConfigsWithoutDatalakeHdfs() {
        when(hdfsConfigHelper.getAttachedDatalakeHdfsUrlForHybridDatahub(templateProcessor, source)).thenReturn(Optional.empty());
        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(templateProcessor, source);
        assertThat(result).isEmpty();
    }

    @Test
    void getServiceConfigsWithDatalakeHdfs() {
        when(hdfsConfigHelper.getAttachedDatalakeHdfsUrlForHybridDatahub(templateProcessor, source)).thenReturn(Optional.of("hdfs://ns1"));
        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(templateProcessor, source);
        assertThat(result).containsExactly(config(TEZ_HISTORY_LOGGING_PROTO_BASE_DIR_KEY, "hdfs://ns1" + TEZ_HISTORY_LOGGING_PROTO_BASE_DIR_VALUE));
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
