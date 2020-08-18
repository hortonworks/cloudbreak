package com.sequenceiq.cloudbreak.cloud.openstack.heat;

import static com.google.common.collect.Lists.newArrayList;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Keypair;
import org.openstack4j.model.heat.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.NetworkConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkDeletionRequest;
import com.sequenceiq.cloudbreak.cloud.network.NetworkCidr;
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackClient;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackUtils;
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class OpenStackHeatNetworkConnector implements NetworkConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackHeatNetworkConnector.class);

    private static final long OPERATION_TIMEOUT = 60L;

    @Inject
    private OpenStackClient openStackClient;

    @Inject
    private HeatNetworkTemplateBuilder heatNetworkTemplateBuilder;

    @Inject
    private OpenStackUtils openStackUtil;

    @Override
    public CreatedCloudNetwork createNetworkWithSubnets(NetworkCreationRequest networkCreationRequest) {
        HeatNetworkTemplateBuilder.ModelContext modelContext = new HeatNetworkTemplateBuilder.ModelContext();

        CloudContext cloudContext = getCloudContext(networkCreationRequest);
        AuthenticatedContext auth = new AuthenticatedContext(cloudContext, networkCreationRequest.getCloudCredential());
        String stackName = openStackUtil.getStackName(auth);

        String heatTemplate = heatNetworkTemplateBuilder.build(modelContext);
        Map<String, String> parameters = heatNetworkTemplateBuilder.buildParameters(auth);
        OSClient<?> client = openStackClient.createOSClient(auth);
        //InstanceAuthentication instanceAuthentication = new InstanceAuthentication();
        List<CloudResourceStatus> resources;
        Stack existingStack = client.heat().stacks().getStackByName(stackName);

        if (existingStack == null) {
            Stack heatStack = client
                    .heat()
                    .stacks()
                    .create(Builders.stack().name(stackName).template(heatTemplate).disableRollback(false)
                            .parameters(parameters).timeoutMins(OPERATION_TIMEOUT).build());
            //List<CloudResource> cloudResources = collectResources(authenticatedContext, notifier, heatStack, stack, neutronNetworkView);
            //resources = check(auth, cloudResources);
        } else {
            LOGGER.debug("Heat stack already exists: {}", existingStack.getName());
            //List<CloudResource> cloudResources = collectResources(authenticatedContext, notifier, existingStack, stack, neutronNetworkView);
            //resources = check(auth, cloudResources);
        }
        //LOGGER.debug("Launched resources: {}", resources);

        return null;
    }

    private List<CloudResourceStatus> check(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        List<CloudResourceStatus> results = newArrayList();
        OSClient<?> client = openStackClient.createOSClient(authenticatedContext);
        String stackName = openStackUtil.getStackName(authenticatedContext);
        List<CloudResource> osResourceList = openStackClient.getResources(stackName, authenticatedContext.getCloudCredential());
        List<CloudResource> otherResources = newArrayList();
        CloudResourceStatus heatStatus = null;
        for (CloudResource resource : resources) {
            if (resource.getType() == ResourceType.HEAT_STACK) {
                heatStatus = checkByResourceType(authenticatedContext, client, stackName, osResourceList, resource);
                results.add(heatStatus);
            } else {
                otherResources.add(resource);
            }
        }
        if (heatStatus != null) {
            for (CloudResource resource : otherResources) {
                if (heatStatus.getStatus() == ResourceStatus.CREATED) {
                    results.add(checkByResourceType(authenticatedContext, client, stackName, osResourceList, resource));
                } else {
                    results.add(new CloudResourceStatus(resource, ResourceStatus.CREATED));
                }
            }
        }
        return results;
    }

    private CloudResourceStatus checkByResourceType(AuthenticatedContext authenticatedContext, OSClient<?> client,
                                                    String stackName, Collection<CloudResource> list, CloudResource resource) {
        CloudResourceStatus result = null;
        switch (resource.getType()) {
            case HEAT_STACK:
                String heatStackId = resource.getName();
                LOGGER.debug("Checking OpenStack Heat stack status of: {}", stackName);
                Stack heatStack = Optional.ofNullable(client.heat().stacks().getDetails(stackName, heatStackId))
                        .orElseThrow(() -> new CloudConnectorException("Stack not found on provider by id: " + heatStackId));
                result = openStackUtil.heatStatus(resource, heatStack);
                break;
            case OPENSTACK_NETWORK:
                //result = checkResourceStatus(authenticatedContext, stackName, list, resource, ResourceType.OPENSTACK_NETWORK);
                break;
            case OPENSTACK_SUBNET:
                //result = checkResourceStatus(authenticatedContext, stackName, list, resource, ResourceType.OPENSTACK_SUBNET);
                break;
            case OPENSTACK_ROUTER:
            case OPENSTACK_INSTANCE:
            case OPENSTACK_PORT:
            case OPENSTACK_ATTACHED_DISK:
            case OPENSTACK_SECURITY_GROUP:
            case OPENSTACK_FLOATING_IP:
                break;
            default:
                throw new CloudConnectorException(String.format("Invalid resource type: %s", resource.getType()));
        }
        return result;
    }

    private void createKeyPair(AuthenticatedContext authenticatedContext, InstanceAuthentication instanceAuthentication,
        OSClient<?> client) {
        KeystoneCredentialView keystoneCredential = openStackClient.createKeystoneCredential(authenticatedContext);

        String keyPairName = keystoneCredential.getKeyPairName();
        if (client.compute().keypairs().get(keyPairName) == null) {
            try {
                Keypair keyPair = client.compute().keypairs().create(keyPairName, instanceAuthentication.getPublicKey());
                LOGGER.debug("Keypair has been created: {}", keyPair);
            } catch (Exception e) {
                LOGGER.warn("Failed to create keypair", e);
                throw new CloudConnectorException(e.getMessage(), e);
            }
        } else {
            LOGGER.debug("Keypair already exists: {}", keyPairName);
        }
    }

    private CloudContext getCloudContext(NetworkCreationRequest networkRequest) {
        return new CloudContext(
                networkRequest.getEnvId(),
                networkRequest.getEnvName(),
                CloudPlatform.OPENSTACK.name(),
                OpenStackConstants.OpenStackVariant.HEAT.variant().value(),
                location(networkRequest.getRegion()),
                networkRequest.getCreatorCrn(),
                networkRequest.getAccountId());
    }

    @Override
    public void deleteNetworkWithSubnets(NetworkDeletionRequest networkDeletionRequest) {

    }

    @Override
    public NetworkCidr getNetworkCidr(Network network, CloudCredential credential) {
        return null;
    }

    @Override
    public SubnetSelectionResult chooseSubnets(Collection<CloudSubnet> subnetMetas, SubnetSelectionParameters subnetSelectionParameters) {
        return null;
    }

    @Override
    public Platform platform() {
        return OpenStackConstants.OPENSTACK_PLATFORM;
    }

    @Override
    public Variant variant() {
        return OpenStackConstants.OpenStackVariant.HEAT.variant();
    }
}
