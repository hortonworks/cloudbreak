package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@ExtendWith(MockitoExtension.class)
class HdfsConfigHelperTest {

    @InjectMocks
    private HdfsConfigHelper underTest;

    @Mock
    private CmTemplateProcessor templateProcessor;

    @Mock
    private TemplatePreparationObject source;

    @BeforeEach
    void setUp() {
    }

    @Test
    void getHdfsUrlForHybridDhWithNoNodesYet() {
        when(templateProcessor.isHybridDatahub(source)).thenReturn(true);
        when(templateProcessor.getHostsWithComponent(HdfsRoles.NAMENODE)).thenReturn(List.of());

        Optional<String> result = underTest.getHdfsUrl(templateProcessor, source);

        assertThat(result)
                .isPresent()
                .contains("hdfs://" + HdfsConfigHelper.HYBRID_DH_NAME_SERVICE);
    }

    @Test
    void getHdfsUrlForNonHybridDhWithNoNodesYet() {
        when(templateProcessor.isHybridDatahub(source)).thenReturn(false);
        when(templateProcessor.getHostsWithComponent(HdfsRoles.NAMENODE)).thenReturn(List.of());

        Optional<String> result = underTest.getHdfsUrl(templateProcessor, source);

        assertThat(result)
                .isPresent()
                .contains("hdfs://" + HdfsConfigHelper.DEFAULT_NAME_SERVICE);
    }

    @Test
    void getHdfsUrlWithSingleNodeAndDefaultPort() {
        when(templateProcessor.getRoleConfig(HdfsRoles.HDFS, HdfsRoles.NAMENODE, "namenode_port")).thenReturn(Optional.empty());
        when(templateProcessor.getHostsWithComponent(HdfsRoles.NAMENODE)).thenReturn(List.of("node1"));

        Optional<String> result = underTest.getHdfsUrl(templateProcessor, source);

        assertThat(result)
                .isPresent()
                .contains("hdfs://node1:" + HdfsConfigHelper.DEFAULT_NAMENODE_PORT);
    }

    @Test
    void getHdfsUrlWithSingleNodeAndNonDefaultPort() {
        when(templateProcessor.getRoleConfig(HdfsRoles.HDFS, HdfsRoles.NAMENODE, "namenode_port"))
                .thenReturn(Optional.of(new ApiClusterTemplateConfig().value("1234")));
        when(templateProcessor.getHostsWithComponent(HdfsRoles.NAMENODE)).thenReturn(List.of("node1"));

        Optional<String> result = underTest.getHdfsUrl(templateProcessor, source);

        assertThat(result)
                .isPresent()
                .contains("hdfs://node1:1234");
    }

    @Test
    void getHdfsUrlForNonHybridDhWithMultipleNodes() {
        when(templateProcessor.isHybridDatahub(source)).thenReturn(false);
        when(templateProcessor.getHostsWithComponent(HdfsRoles.NAMENODE)).thenReturn(List.of("node1", "node2"));

        Optional<String> result = underTest.getHdfsUrl(templateProcessor, source);

        assertThat(result)
                .isPresent()
                .contains("hdfs://" + HdfsConfigHelper.DEFAULT_NAME_SERVICE);
    }

    @Test
    void getHdfsUrlForHybridDhWithMultipleNodes() {
        when(templateProcessor.isHybridDatahub(source)).thenReturn(true);
        when(templateProcessor.getHostsWithComponent(HdfsRoles.NAMENODE)).thenReturn(List.of("node1", "node2"));

        Optional<String> result = underTest.getHdfsUrl(templateProcessor, source);

        assertThat(result)
                .isPresent()
                .contains("hdfs://" + HdfsConfigHelper.HYBRID_DH_NAME_SERVICE);
    }

}
