package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.getSafetyValveProperty;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HdfsNameNode.HDFS_NAMENODE_NAMESERVICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.sdx.RdcView;
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
    void getHdfsUrlForDhWithNoNameNodes() {
        when(templateProcessor.getHostsWithComponent(HdfsRoles.NAMENODE)).thenReturn(List.of());

        Optional<String> result = underTest.getHdfsUrl(templateProcessor, source);

        assertThat(result).isEmpty();
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

    @Test
    void getNameServiceConfigSafetyValveValueWithNameservice() {
        RdcView rdcView = mock();
        when(rdcView.getRoleConfigs(HdfsRoles.HDFS, HdfsRoles.NAMENODE)).thenReturn(Map.of(
                HDFS_NAMENODE_NAMESERVICE, "ns1",
                "dfs.ha.namenodes.conf1", "value1",
                "dfs.namenode.conf2", "value2",
                "otherconf", "othervalue"
        ));

        String result = underTest.getNameServiceConfigSafetyValveValue(rdcView);

        assertThat(result)
                .contains(getSafetyValveProperty(
                        "dfs.client.failover.proxy.provider.ns1",
                        "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider"))
                .contains(getSafetyValveProperty("dfs.ha.namenodes.conf1", "value1"))
                .contains(getSafetyValveProperty("dfs.namenode.conf2", "value2"))
                .doesNotContain("othervalue");
    }

    @Test
    void getNameServiceConfigSafetyValveValueWithoutNameservice() {
        RdcView rdcView = mock();
        when(rdcView.getRoleConfigs(HdfsRoles.HDFS, HdfsRoles.NAMENODE)).thenReturn(Map.of());

        String result = underTest.getNameServiceConfigSafetyValveValue(rdcView);

        assertThat(result).isEmpty();
    }

}
