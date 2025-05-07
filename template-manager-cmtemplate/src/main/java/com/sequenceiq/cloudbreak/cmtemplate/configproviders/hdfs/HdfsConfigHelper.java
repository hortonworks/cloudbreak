package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs;

import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.BlueprintHybridOption;
import com.sequenceiq.cloudbreak.sdx.RdcView;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.DatalakeView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class HdfsConfigHelper {

    public static final String HDFS_CLIENT_CONFIG_SAFETY_VALVE = "hdfs_client_config_safety_valve";

    public static final String DEFAULT_NAMENODE_PORT = "8020";

    protected static final String HYBRID_DH_NAME_SERVICE = "nshybrid";

    private static final String DEFAULT_NAME_SERVICE = "ns1";

    public boolean isNamenodeHA(TemplatePreparationObject source) {
        return source.getHostGroupsWithComponent(HdfsRoles.NAMENODE)
                .mapToInt(HostgroupView::getNodeCount)
                .sum() > 1;
    }

    public Set<String> nameNodeFQDNs(TemplatePreparationObject source) {
        return source.getHostGroupsWithComponent(HdfsRoles.NAMENODE)
                .flatMap(hostGroup -> hostGroup.getHosts().stream())
                .collect(toSet());
    }

    public boolean isDataNodeHA(TemplatePreparationObject source) {
        return source.getHostGroupsWithComponent(HdfsRoles.DATANODE)
                .mapToInt(HostgroupView::getNodeCount)
                .sum() > 1;
    }

    public String getNameService(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        return isHybridDatahub(templateProcessor, source) ? HYBRID_DH_NAME_SERVICE : DEFAULT_NAME_SERVICE;
    }

    public boolean isHybridDatahub(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        return StackType.WORKLOAD.equals(source.getStackType())
                && source.getDatalakeView().isPresent()
                && BlueprintHybridOption.BURST_TO_CLOUD.equals(source.getBlueprintView().getHybridOption());
    }

    public Optional<String> getHdfsUrl(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<String> nameNodeHostNames = templateProcessor.getHostsWithComponent(HdfsRoles.NAMENODE);
        return nameNodeHostNames.size() > 1
                ? Optional.of("hdfs://" + getNameService(templateProcessor, source))
                : nameNodeHostNames.stream().map(nameNode -> String.format("hdfs://%s:%s", nameNode, getNameNodePort(templateProcessor))).findFirst();
    }

    private String getNameNodePort(CmTemplateProcessor templateProcessor) {
        return templateProcessor.getRoleConfig(HdfsRoles.HDFS, HdfsRoles.NAMENODE, "namenode_port")
                .map(ApiClusterTemplateConfig::getValue)
                .orElse(DEFAULT_NAMENODE_PORT);
    }

    public Optional<String> getAttachedDatalakeHdfsUrlForHybridDatahub(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        return isHybridDatahub(templateProcessor, source)
                ? getDatalakeHdfsUrl(source.getDatalakeView().get())
                : Optional.empty();
    }

    private Optional<String> getDatalakeHdfsUrl(DatalakeView datalake) {
        Set<String> nameNodes = datalake.getRdcView().getEndpoints(HdfsRoles.HDFS, HdfsRoles.NAMENODE);
        return nameNodes.size() > 1
                ? Optional.of("hdfs://" + getNameService(datalake.getRdcView()))
                : nameNodes.stream().findFirst();
    }

    public String getNameService(RdcView rdcView) {
        return rdcView.getRoleConfigs(HdfsRoles.HDFS, HdfsRoles.NAMENODE)
                .getOrDefault("dfs.federation.namenode.nameservice", DEFAULT_NAME_SERVICE);
    }

}
