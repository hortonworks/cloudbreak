package com.sequenceiq.cloudbreak.cloud;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
class ResourceConnectorTest {

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private DatabaseStack stack;

    @InjectMocks
    private ResourceConnectorImpl underTest;

    @Test
    void getDatabaseServerActiveSslRootCertificateTest() {
        assertThrows(UnsupportedOperationException.class, () -> underTest.getDatabaseServerActiveSslRootCertificate(authenticatedContext, stack));
    }

    private static class ResourceConnectorImpl implements ResourceConnector {

        @Override
        public List<CloudResourceStatus> launch(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier persistenceNotifier,
                AdjustmentTypeWithThreshold adjustmentTypeWithThreshold) {
            return null;
        }

        @Override
        public List<CloudResourceStatus> launchDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack,
                PersistenceNotifier persistenceNotifier) {
            return null;
        }

        @Override
        public void validateUpgradeDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack, PersistenceNotifier persistenceNotifier,
                TargetMajorVersion targetMajorVersion) {
        }

        @Override
        public void upgradeDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack,
                PersistenceNotifier persistenceNotifier, TargetMajorVersion targetMajorVersion, List<CloudResource> resources) {
        }

        @Override
        public void startDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack) {

        }

        @Override
        public void stopDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack) {

        }

        @Override
        public ExternalDatabaseStatus getDatabaseServerStatus(AuthenticatedContext authenticatedContext, DatabaseStack stack) {
            return null;
        }

        @Override
        public TlsInfo getTlsInfo(AuthenticatedContext authenticatedContext, CloudStack cloudStack) {
            return null;
        }

        @Override
        public String getStackTemplate() {
            return null;
        }

        @Override
        public String getDBStackTemplate(DatabaseStack databaseStack) {
            return null;
        }

        @Override
        public List<CloudResourceStatus> launchLoadBalancers(AuthenticatedContext authenticatedContext, CloudStack stack,
                PersistenceNotifier persistenceNotifier) {
            return null;
        }

        @Override
        public List<CloudResource> collectResourcesToRemove(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources,
                List<CloudInstance> vms) {
            return null;
        }

        @Override
        public List<CloudResourceStatus> downscale(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources,
                List<CloudInstance> vms, List<CloudResource> resourcesToRemove) {
            return null;
        }

        @Override
        public List<CloudResourceStatus> upscale(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources,
                AdjustmentTypeWithThreshold adjustmentTypeWithThreshold) {
            return null;
        }

        @Override
        public List<CloudResourceStatus> update(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources, UpdateType type) {
            return null;
        }

        @Override
        public void updateUserData(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources,
                Map<InstanceGroupType, String> userData) {

        }

        @Override
        public void checkUpdate(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) {
        }

        @Override
        public List<CloudResourceStatus> terminateDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack, List<CloudResource> resources,
                PersistenceNotifier persistenceNotifier, boolean force) {
            return null;
        }

        @Override
        public List<CloudResourceStatus> terminate(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> cloudResources) {
            return null;
        }

        @Override
        public List<CloudResourceStatus> check(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
            return null;
        }

        @Override
        public void updateDatabaseRootPassword(AuthenticatedContext authenticatedContext, DatabaseStack databaseStack, String newPassword) {

        }
    }

}
