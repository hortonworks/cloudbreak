package com.sequenceiq.cloudbreak.cluster.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

public interface ClusterSetupService {

    String BEAN_POST_TAG = "PRE_CLUSTER";

    void waitForServer() throws CloudbreakException;

    Cluster buildCluster(Map<HostGroup, List<InstanceMetaData>> instanceMetaDataByHostGroup, TemplatePreparationObject templatePreparationObject,
            Set<HostMetadata> hostsInCluster);

    void configureSmartSense();

    void waitForHosts(Set<HostMetadata> hostsInCluster);

    void waitForServices(int requestId) throws CloudbreakException;
}
