package com.sequenceiq.cloudbreak.cluster.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

public interface ClusterSetupService {

    String BEAN_POST_TAG = "PRE_CLUSTER";

    void waitForServer() throws CloudbreakException, ClusterClientInitException;

    String prepareTemplate(
        Map<HostGroup, List<InstanceMetaData>> instanceMetaDataByHostGroup,
        TemplatePreparationObject templatePreparationObject,
        String sdxContext,
        String sdxStackCrn,
        KerberosConfig kerberosConfig);

    Cluster buildCluster(
        Map<HostGroup, List<InstanceMetaData>> instanceMetaDataByHostGroup,
        TemplatePreparationObject templatePreparationObject,
        String sdxContext,
        String sdxStackCrn,
        Telemetry telemetry,
        KerberosConfig kerberosConfig,
        String template);

    void waitForHosts(Set<InstanceMetaData> hostsInCluster) throws ClusterClientInitException;

    void waitForServices(int requestId) throws CloudbreakException;

    String getSdxContext();

    void setupProxy(ProxyConfig proxyConfig);
}
