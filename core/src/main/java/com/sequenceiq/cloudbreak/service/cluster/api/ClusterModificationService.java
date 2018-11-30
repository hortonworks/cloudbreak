package com.sequenceiq.cloudbreak.service.cluster.api;

import java.util.Collection;
import java.util.Map;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

public interface ClusterModificationService {

    void upscaleCluster(Stack stack, HostGroup hostGroup, Collection<HostMetadata> hostMetadata) throws CloudbreakException;

    void stopCluster(Stack stack) throws CloudbreakException;

    int startCluster(Stack stack) throws CloudbreakException;

    Map<String, String> gatherInstalledComponents(Stack stack, String hostname);

    void stopComponents(Stack stack, Map<String, String> components, String hostname) throws CloudbreakException;

    void ensureComponentsAreStopped(Stack stack, Map<String, String> components, String hostname) throws CloudbreakException;

    void initComponents(Stack stack, Map<String, String> components, String hostname) throws CloudbreakException;

    void installComponents(Stack stack, Map<String, String> components, String hostname) throws CloudbreakException;

    void regenerateKerberosKeytabs(Stack stack, String hostname) throws CloudbreakException;

    void startComponents(Stack stack, Map<String, String> components, String hostname) throws CloudbreakException;

    void restartAll(Stack stack) throws CloudbreakException;
}
