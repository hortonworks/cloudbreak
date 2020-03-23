package com.sequenceiq.cloudbreak.cluster.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

public interface ClusterModificationService {

    List<String> upscaleCluster(HostGroup hostGroup, Collection<InstanceMetaData> metas) throws CloudbreakException;

    void stopCluster() throws CloudbreakException;

    int startCluster(Set<InstanceMetaData> hostsInCluster) throws CloudbreakException;

    Map<String, String> getComponentsByCategory(String blueprintName, String hostGroupName);

    String getStackRepositoryJson(StackRepoDetails repoDetails, String stackRepoId);

    Map<String, String> gatherInstalledComponents(String hostname);

    void stopComponents(Map<String, String> components, String hostname) throws CloudbreakException;

    void ensureComponentsAreStopped(Map<String, String> components, String hostname) throws CloudbreakException;

    void initComponents(Map<String, String> components, String hostname) throws CloudbreakException;

    void installComponents(Map<String, String> components, String hostname) throws CloudbreakException;

    void regenerateKerberosKeytabs(String hostname, KerberosConfig kerberosConfig) throws CloudbreakException;

    void startComponents(Map<String, String> components, String hostname) throws CloudbreakException;

    void cleanupCluster(Telemetry telemetry) throws CloudbreakException;

    void restartAll() throws CloudbreakException;

    void upgradeClusterRuntime(Set<ClusterComponent> components) throws CloudbreakException;
}
