package com.sequenceiq.cloudbreak.cluster.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatus;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerState;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

public interface ClusterApi {

    String AMBARI = "AMBARI";

    String CLOUDERA_MANAGER = "CLOUDERA_MANAGER";

    default void waitForServer(Stack stack) throws CloudbreakException, ClusterClientInitException {
        clusterSetupService().waitForServer();
    }

    default String getSdxContext() {
        return clusterSetupService().getSdxContext();
    }

    default void waitForHosts(Set<InstanceMetaData> hostsInCluster) throws ClusterClientInitException {
        clusterSetupService().waitForHosts(hostsInCluster);
    }

    default void waitForServices(int requestId) throws CloudbreakException {
        clusterSetupService().waitForServices(requestId);
    }

    default void replaceUserNamePassword(String newUserName, String newPassword) throws CloudbreakException {
        clusterSecurityService().replaceUserNamePassword(newUserName, newPassword);
    }

    default void updateUserNamePassword(String newPassword) throws CloudbreakException {
        clusterSecurityService().updateUserNamePassword(newPassword);
    }

    default void prepareSecurity() {
        clusterSecurityService().prepareSecurity();
    }

    default void disableSecurity() {
        clusterSecurityService().disableSecurity();
    }

    default void changeOriginalCredentialsAndCreateCloudbreakUser(boolean ldapConfigured) throws CloudbreakException {
        clusterSecurityService().changeOriginalCredentialsAndCreateCloudbreakUser(ldapConfigured);
    }

    default List<String> upscaleCluster(HostGroup hostGroup, Collection<InstanceMetaData> metas) throws CloudbreakException {
        return clusterModificationService().upscaleCluster(hostGroup, metas);
    }

    default void stopCluster() throws CloudbreakException {
        clusterModificationService().stopCluster();
    }

    default int startCluster(Set<InstanceMetaData> hostsInCluster) throws CloudbreakException {
        return clusterModificationService().startCluster(hostsInCluster);
    }

    default Map<String, String> gatherInstalledComponents(String hostname) {
        return clusterModificationService().gatherInstalledComponents(hostname);
    }

    default void ensureComponentsAreStopped(Map<String, String> components, String hostname) throws CloudbreakException {
        clusterModificationService().ensureComponentsAreStopped(components, hostname);
    }

    default void initComponents(Map<String, String> components, String hostname) throws CloudbreakException {
        clusterModificationService().initComponents(components, hostname);
    }

    default void stopComponents(Map<String, String> components, String hostname) throws CloudbreakException {
        clusterModificationService().stopComponents(components, hostname);
    }

    default void installComponents(Map<String, String> components, String hostname) throws CloudbreakException {
        clusterModificationService().installComponents(components, hostname);
    }

    default void startComponents(Map<String, String> components, String hostname) throws CloudbreakException {
        clusterModificationService().startComponents(components, hostname);
    }

    default void cleanupCluster(Telemetry telemetry) throws CloudbreakException {
        clusterModificationService().cleanupCluster(telemetry);
    }

    default void restartAll() throws CloudbreakException {
        clusterModificationService().restartAll();
    }

    default ClusterStatus getStatus(boolean blueprintPresent) {
        return clusterStatusService().getStatus(blueprintPresent).getClusterStatus();
    }

    default Map<String, ClusterManagerState.ClusterManagerStatus> getHostStatuses() {
        return clusterStatusService().getHostStatuses();
    }

    ClusterSetupService clusterSetupService();

    ClusterModificationService clusterModificationService();

    ClusterSecurityService clusterSecurityService();

    ClusterStatusService clusterStatusService();

    ClusterDecomissionService clusterDecomissionService();
}
