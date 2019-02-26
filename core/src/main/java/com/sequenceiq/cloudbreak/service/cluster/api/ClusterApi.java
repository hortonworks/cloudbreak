package com.sequenceiq.cloudbreak.service.cluster.api;

import java.util.Collection;

import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

public interface ClusterApi {

    default void waitForServer(Stack stack) throws CloudbreakException {
        clusterSetupService().waitForServer(stack);
    }

    default void buildCluster(Stack stack) {
        clusterSetupService().buildCluster(stack);
    }

    default void waitForHosts(Stack stack) {
        clusterSetupService().waitForHosts(stack);
    }

    default boolean available(Stack stack) {
        return clusterSetupService().available(stack);
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

    default void changeOriginalAmbariCredentialsAndCreateCloudbreakUser(Stack stack) throws CloudbreakException {
        clusterSecurityService().changeOriginalAmbariCredentialsAndCreateCloudbreakUser(stack);
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

    ClusterSetupService clusterSetupService();

    ClusterModificationService clusterModificationService();

    ClusterSecurityService clusterSecurityService();

}
