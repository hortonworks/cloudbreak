package com.sequenceiq.cloudbreak.service.cluster.api;

import java.util.Collection;
import java.util.Map;

import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

public interface ClusterApi {

    default void waitForServer(Stack stack) throws CloudbreakException {
        clusterSetupService().waitForServer(stack);
    }

    default void buildCluster(Stack stack) {
        clusterSetupService().buildCluster(stack);
    }

    default void waitForHosts(Stack stack) throws CloudbreakSecuritySetupException {
        clusterSetupService().waitForHosts(stack);
    }

    default void waitForServices(Stack stack, int requestId) throws CloudbreakException {
        clusterSetupService().waitForServices(stack, requestId);
    }

    default void replaceUserNamePassword(Stack stackId, String newUserName, String newPassword) throws CloudbreakException {
        clusterSecurityService().replaceUserNamePassword(stackId, newUserName, newPassword);
    }

    default void updateUserNamePassword(Stack stack, String newPassword) throws CloudbreakException {
        clusterSecurityService().updateUserNamePassword(stack, newPassword);
    }

    default void prepareSecurity(Stack stack) {
        clusterSecurityService().prepareSecurity(stack);
    }

    default void disableSecurity(Stack stack) {
        clusterSecurityService().disableSecurity(stack);
    }

    default void changeOriginalCredentialsAndCreateCloudbreakUser(Stack stack) throws CloudbreakException {
        clusterSecurityService().changeOriginalCredentialsAndCreateCloudbreakUser(stack);
    }

    default void upscaleCluster(Stack stack, HostGroup hostGroup, Collection<HostMetadata> hostMetadata) throws CloudbreakException {
        clusterModificationService().upscaleCluster(stack, hostGroup, hostMetadata);
    }

    default void stopCluster(Stack stack) throws CloudbreakException {
        clusterModificationService().stopCluster(stack);
    }

    default int startCluster(Stack stack) throws CloudbreakException {
        return clusterModificationService().startCluster(stack);
    }

    default Map<String, String> gatherInstalledComponents(Stack stack, String hostname) {
        return clusterModificationService().gatherInstalledComponents(stack, hostname);
    }

    default void ensureComponentsAreStopped(Stack stack, Map<String, String> components, String hostname) throws CloudbreakException {
        clusterModificationService().ensureComponentsAreStopped(stack, components, hostname);
    }

    default void initComponents(Stack stack, Map<String, String> components, String hostname) throws CloudbreakException {
        clusterModificationService().initComponents(stack, components, hostname);
    }

    default void stopComponents(Stack stack, Map<String, String> components, String hostname) throws CloudbreakException {
        clusterModificationService().stopComponents(stack, components, hostname);
    }

    default void installComponents(Stack stack, Map<String, String> components, String hostname) throws CloudbreakException {
        clusterModificationService().installComponents(stack, components, hostname);
    }

    default void regenerateKerberosKeytabs(Stack stack, String hostname) throws CloudbreakException {
        clusterModificationService().regenerateKerberosKeytabs(stack, hostname);
    }

    default void startComponents(Stack stack, Map<String, String> components, String hostname) throws CloudbreakException {
        clusterModificationService().startComponents(stack, components, hostname);
    }

    default void restartAll(Stack stack) throws CloudbreakException {
        clusterModificationService().restartAll(stack);
    }

    ClusterSetupService clusterSetupService();

    ClusterModificationService clusterModificationService();

    ClusterSecurityService clusterSecurityService();

    String clusterVariant();

}
