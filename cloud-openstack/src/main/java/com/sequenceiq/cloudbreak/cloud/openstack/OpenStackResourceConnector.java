package com.sequenceiq.cloudbreak.cloud.openstack;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Keypair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.TemplatingNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.openstack.client.OpenStackClient;
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView;
import com.sequenceiq.cloudbreak.cloud.template.AbstractResourceConnector;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class OpenStackResourceConnector extends AbstractResourceConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackResourceConnector.class);

    @Inject
    private OpenStackClient openStackClient;

    @Override
    protected boolean isCloudResourceAndCloudInstanceEquals(CloudInstance instance, CloudResource resource) {
        return instance.getInstanceId().equalsIgnoreCase(resource.getInstanceId());
    }

    @Override
    protected List<CloudResource> collectProviderSpecificResources(List<CloudResource> resources, List<CloudInstance> vms) {
        return List.of();
    }

    @Override
    public List<CloudResourceStatus> launchLoadBalancers(AuthenticatedContext authenticatedContext, CloudStack stack,
            PersistenceNotifier persistenceNotifier) throws Exception {
        return List.of();
    }

    @Override
    public void updateUserData(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources,
            Map<InstanceGroupType, String> userData) throws Exception {
        LOGGER.info("Update userdata is not implemented on OpenStack!");
    }

    @Override
    public TlsInfo getTlsInfo(AuthenticatedContext authenticatedContext, CloudStack cloudStack) {
        return new TlsInfo(false);
    }

    @Override
    public String getStackTemplate() throws TemplatingNotSupportedException {
        throw new TemplatingNotSupportedException();
    }

    @Override
    public String getDBStackTemplate(DatabaseStack databaseStack) throws TemplatingNotSupportedException {
        throw new TemplatingNotSupportedException();
    }

    @Override
    public void updateDatabaseRootPassword(AuthenticatedContext authenticatedContext, DatabaseStack databaseStack, String newPassword) {
        throw new UnsupportedOperationException("Database root password update is not supported on OpenStack!");
    }

    @Override
    public List<CloudResourceStatus> launch(AuthenticatedContext auth, CloudStack stack, PersistenceNotifier notifier,
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold) throws Exception {
        createKeyPair(auth, stack, openStackClient.createOSClient(auth));
        return super.launch(auth, stack, notifier, adjustmentTypeWithThreshold);
    }

    @Override
    public List<CloudResourceStatus> terminate(AuthenticatedContext auth, CloudStack stack, List<CloudResource> cloudResources) throws Exception {
        deleteKeyPair(auth, openStackClient.createOSClient(auth));
        return super.terminate(auth, stack, cloudResources);
    }

    @Override
    protected ResourceType getDiskResourceType() {
        return ResourceType.OPENSTACK_ATTACHED_DISK;
    }

    private void createKeyPair(AuthenticatedContext auth, CloudStack stack, OSClient<?> client) {
        KeystoneCredentialView keystoneCredential = openStackClient.createKeystoneCredential(auth);

        String keyPairName = keystoneCredential.getKeyPairName();
        if (client.compute().keypairs().get(keyPairName) == null) {
            try {
                Keypair keyPair = client.compute().keypairs().create(keyPairName, stack.getInstanceAuthentication().getPublicKey());
                LOGGER.debug("Keypair has been created: {}", keyPair);
            } catch (Exception e) {
                LOGGER.warn("Failed to create keypair", e);
                throw new CloudConnectorException(e.getMessage(), e);
            }
        } else {
            LOGGER.debug("Keypair already exists: {}", keyPairName);
        }
    }

    private void deleteKeyPair(AuthenticatedContext authenticatedContext, OSClient<?> client) {
        KeystoneCredentialView keystoneCredential = openStackClient.createKeystoneCredential(authenticatedContext);
        String keyPairName = keystoneCredential.getKeyPairName();
        if (client.compute().keypairs().get(keyPairName) != null) {
            client.compute().keypairs().delete(keyPairName);
            LOGGER.debug("Keypair has been deleted: {}", keyPairName);
        } else {
            LOGGER.debug("Keypair does not exist: {}", keyPairName);
        }
    }
}
