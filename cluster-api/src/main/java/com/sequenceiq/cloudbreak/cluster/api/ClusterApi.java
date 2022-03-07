package com.sequenceiq.cloudbreak.cluster.api;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.cluster.model.ParcelOperationStatus;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatus;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

public interface ClusterApi {

    String CLOUDERA_MANAGER = "CLOUDERA_MANAGER";

    default void waitForServer(Stack stack, boolean defaultClusterManagerAuth) throws CloudbreakException, ClusterClientInitException {
        clusterSetupService().waitForServer(defaultClusterManagerAuth);
    }

    default String getSdxContext() {
        return clusterSetupService().getSdxContext();
    }

    default ExtendedPollingResult waitForHosts(Set<InstanceMetaData> hostsInCluster) throws ClusterClientInitException {
        return clusterSetupService().waitForHosts(hostsInCluster);
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

    default List<String> upscaleCluster(Map<HostGroup, Set<InstanceMetaData>> instanceMetaDatasByHostGroup) throws CloudbreakException {
        return clusterModificationService().upscaleCluster(instanceMetaDatasByHostGroup);
    }

    default void upgradeClusterRuntime(Set<ClusterComponent> components, boolean patchUpgrade, Optional<String> remoteDataContext) throws CloudbreakException {
        clusterModificationService().upgradeClusterRuntime(components, patchUpgrade, remoteDataContext);
    }

    default void updateServiceConfig(String serviceType, Map<String, String> config) throws CloudbreakException {
        clusterModificationService().updateServiceConfig(serviceType, config);
    }

    default void stopCluster(boolean disableKnoxAutorestart) throws CloudbreakException {
        clusterModificationService().stopCluster(disableKnoxAutorestart);
    }

    default int startCluster() throws CloudbreakException {
        return clusterModificationService().startCluster();
    }

    default void startClusterMgmtServices() throws CloudbreakException {
        clusterModificationService().startClusterMgmtServices();
    }

    default int startClusterServices() throws CloudbreakException {
        return clusterModificationService().deployConfigAndStartClusterServices();
    }

    default Map<String, String> gatherInstalledParcels(String stackName) {
        return clusterModificationService().gatherInstalledParcels(stackName);
    }

    default void downloadAndDistributeParcels(Set<ClusterComponent> components, boolean patchUpgrade) throws CloudbreakException {
        clusterModificationService().downloadAndDistributeParcels(components, patchUpgrade);
    }

    default ParcelOperationStatus removeUnusedParcels(Set<ClusterComponent> usedParcelComponents, Set<String> parcelNamesFromImage)
            throws CloudbreakException {
        return clusterModificationService().removeUnusedParcels(usedParcelComponents, parcelNamesFromImage);
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

    default void restartAll(boolean withMgmtServices) throws CloudbreakException {
        clusterModificationService().restartAll(withMgmtServices);
    }

    default Optional<String> getRoleConfigValueByServiceType(String clusterName, String roleConfigGroup, String serviceType, String configName) {
        return clusterModificationService().getRoleConfigValueByServiceType(clusterName, roleConfigGroup, serviceType, configName);
    }

    default void rotateHostCertificates(String sshUser, KeyPair sshKeyPair, String subAltName) throws CloudbreakException {
        clusterSecurityService().rotateHostCertificates(sshUser, sshKeyPair, subAltName);
    }

    default ClusterStatus getStatus(boolean blueprintPresent) {
        return clusterStatusService().getStatus(blueprintPresent).getClusterStatus();
    }

    ClusterSetupService clusterSetupService();

    ClusterModificationService clusterModificationService();

    ClusterSecurityService clusterSecurityService();

    ClusterStatusService clusterStatusService();

    ClusterDecomissionService clusterDecomissionService();

    ClusterCommissionService clusterCommissionService();

    ClusterDiagnosticsService clusterDiagnosticsService();
}
