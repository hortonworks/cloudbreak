package com.sequenceiq.cloudbreak.cluster.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

public interface ClusterModificationService {

    void upscaleCluster(HostGroup hostGroup, Collection<HostMetadata> hostMetadata, List<InstanceMetaData> metas) throws CloudbreakException;

    void stopCluster() throws CloudbreakException;

    int startCluster(Set<HostMetadata> hostsInCluster) throws CloudbreakException;

    ClusterStatus getStatus(boolean blueprintPresent);

    Map<String, String> getComponentsByCategory(String blueprintName, String hostGroupName);

    String getStackRepositoryJson(StackRepoDetails repoDetails, String stackRepoId);

    Map<String, String> gatherInstalledComponents(String hostname);

    void stopComponents(Map<String, String> components, String hostname) throws CloudbreakException;

    void ensureComponentsAreStopped(Map<String, String> components, String hostname) throws CloudbreakException;

    void initComponents(Map<String, String> components, String hostname) throws CloudbreakException;

    void installComponents(Map<String, String> components, String hostname) throws CloudbreakException;

    void regenerateKerberosKeytabs(String hostname) throws CloudbreakException;

    void startComponents(Map<String, String> components, String hostname) throws CloudbreakException;

    void restartAll() throws CloudbreakException;
}
