package com.sequenceiq.cloudbreak.cluster.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.cluster.status.ClusterStatus;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

public interface ClusterApi {

    String AMBARI = "AMBARI";

    String CLOUDERA_MANAGER = "CLOUDERA_MANAGER";

    default void waitForServer(Stack stack) throws CloudbreakException {
        clusterSetupService().waitForServer();
    }

    default Cluster buildCluster(Map<HostGroup, List<InstanceMetaData>> instanceMetaDataByHostGroup, TemplatePreparationObject templatePreparationObject,
            Set<HostMetadata> hostsInCluster) {
        return clusterSetupService().buildCluster(instanceMetaDataByHostGroup, templatePreparationObject, hostsInCluster);
    }

    default void configureSmartSense() {
        clusterSetupService().configureSmartSense();
    }

    default void waitForHosts(Stack stack, Set<HostMetadata> hostsInCluster) {
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

    default void changeOriginalCredentialsAndCreateCloudbreakUser() throws CloudbreakException {
        clusterSecurityService().changeOriginalCredentialsAndCreateCloudbreakUser();
    }

    default void upscaleCluster(HostGroup hostGroup, Collection<HostMetadata> hostMetadata, List<InstanceMetaData> metas) throws CloudbreakException {
        clusterModificationService().upscaleCluster(hostGroup, hostMetadata, metas);
    }

    default void stopCluster() throws CloudbreakException {
        clusterModificationService().stopCluster();
    }

    default int startCluster(Set<HostMetadata> hostsInCluster) throws CloudbreakException {
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

    default void regenerateKerberosKeytabs(String hostname) throws CloudbreakException {
        clusterModificationService().regenerateKerberosKeytabs(hostname);
    }

    default void startComponents(Map<String, String> components, String hostname) throws CloudbreakException {
        clusterModificationService().startComponents(components, hostname);
    }

    default void restartAll() throws CloudbreakException {
        clusterModificationService().restartAll();
    }

    default ClusterStatus getStatus(boolean blueprintPresent) {
        return clusterModificationService().getStatus(blueprintPresent);
    }

    default Map<String, HostMetadataState> getHostStatuses() {
        return clusterStatusService().getHostStatuses();
    }

    ClusterSetupService clusterSetupService();

    ClusterModificationService clusterModificationService();

    ClusterSecurityService clusterSecurityService();

    ClusterStatusService clusterStatusService();

    ClusterDecomissionService clusterDecomissionService();

}
