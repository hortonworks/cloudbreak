package com.sequenceiq.cloudbreak.cloud.openstack.nativ.compute;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.OS4JException;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.Action;
import org.openstack4j.model.compute.BDMDestType;
import org.openstack4j.model.compute.BDMSourceType;
import org.openstack4j.model.compute.BlockDeviceMappingCreate;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.Server.Status;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.model.compute.builder.BlockDeviceMappingBuilder;
import org.openstack4j.model.compute.builder.ServerCreateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.OpenStackResourceException;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.context.OpenStackContext;
import com.sequenceiq.cloudbreak.cloud.openstack.status.NovaInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView;
import com.sequenceiq.cloudbreak.cloud.openstack.view.NovaInstanceView;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class OpenStackInstanceBuilder extends AbstractOpenStackComputeResourceBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackInstanceBuilder.class);

    @Override
    public List<CloudResource> build(OpenStackContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group,
            List<CloudResource> buildableResource, CloudStack cloudStack) {
        CloudResource resource = buildableResource.get(0);
        try {
            OSClient<?> osClient = createOSClient(auth);
            InstanceTemplate template = getInstanceTemplate(group, privateId);
            CloudResource port = getPort(context.getComputeResources(privateId));
            KeystoneCredentialView osCredential = new KeystoneCredentialView(auth);
            NovaInstanceView novaInstanceView = new NovaInstanceView(context.getName(), template, group.getType(), group.getLoginUserName());
            String imageId = osClient.imagesV2().list(Collections.singletonMap("name", cloudStack.getImage().getImageName())).get(0).getId();
            LOGGER.debug("Selected image id: {}", imageId);
            Map<String, String> metadata = mergeMetadata(novaInstanceView.getMetadataMap(), cloudStack.getTags());
            ServerCreateBuilder serverCreateBuilder = Builders.server()
                    .name(resource.getName())
                    .image(imageId)
                    .flavor(getFlavorId(osClient, novaInstanceView.getFlavor()))
                    .keypairName(osCredential.getKeyPairName())
                    .addMetadata(metadata)
                    .addNetworkPort(port.getStringParameter(OpenStackConstants.PORT_ID))
                    .userData(new String(Base64.encodeBase64(cloudStack.getImage().getUserDataByType(group.getType()).getBytes())));
            BlockDeviceMappingBuilder blockDeviceMappingBuilder = Builders.blockDeviceMapping()
                    .uuid(imageId)
                    .sourceType(BDMSourceType.IMAGE)
                    .deviceName("/dev/vda")
                    .bootIndex(0)
                    .deleteOnTermination(true)
                    .destinationType(BDMDestType.LOCAL);
            serverCreateBuilder = serverCreateBuilder.blockDevice(blockDeviceMappingBuilder.build());
            for (CloudResource computeResource : context.getComputeResources(privateId)) {
                if (computeResource.getType() == ResourceType.OPENSTACK_ATTACHED_DISK) {
                    BlockDeviceMappingCreate blockDeviceMappingCreate = Builders.blockDeviceMapping()
                            .uuid(computeResource.getReference())
                            .deviceName(computeResource.getStringParameter(OpenStackConstants.VOLUME_MOUNT_POINT))
                            .sourceType(BDMSourceType.VOLUME)
                            .destinationType(BDMDestType.VOLUME)
                            .build();
                    serverCreateBuilder.blockDevice(blockDeviceMappingCreate);
                }
            }
            ServerCreate serverCreate = serverCreateBuilder.build();
            Server server = osClient.compute().servers().boot(serverCreate);
            return Collections.singletonList(createPersistedResource(resource, group.getName(), server.getId(),
                    Collections.singletonMap(OpenStackConstants.SERVER, server)));
        } catch (OS4JException ex) {
            LOGGER.info("Failed to create OpenStack instance with privateId: {}", privateId, ex);
            throw new OpenStackResourceException("Instance creation failed", resourceType(), resource.getName(), ex);
        }
    }

    private Map<String, String> mergeMetadata(Map<String, String> metadataMap, Map<String, String> tags) {
        Map<String, String> result = new HashMap<>();
        result.putAll(tags);
        result.putAll(metadataMap);
        return result;
    }

    @Override
    public List<CloudVmInstanceStatus> checkInstances(OpenStackContext context, AuthenticatedContext auth, List<CloudInstance> instances) {
        List<CloudVmInstanceStatus> statuses = Lists.newArrayList();
        OSClient<?> osClient = createOSClient(auth);
        for (CloudInstance instance : instances) {
            Server server = osClient.compute().servers().get(instance.getInstanceId());
            if (server == null) {
                statuses.add(new CloudVmInstanceStatus(instance, InstanceStatus.TERMINATED));
            } else {
                statuses.add(new CloudVmInstanceStatus(instance, NovaInstanceStatus.get(server)));
            }
        }
        return statuses;
    }

    @Override
    public CloudResource delete(OpenStackContext context, AuthenticatedContext auth, CloudResource resource) {
        try {
            OSClient<?> osClient = createOSClient(auth);
            LOGGER.info("About to delete volume: [{}]", resource);
            ActionResponse response = osClient.compute().servers().delete(resource.getReference());
            return checkDeleteResponse(response, resourceType(), auth, resource, "Instance deletion failed");
        } catch (OS4JException ex) {
            throw new OpenStackResourceException("Instance deletion failed", resourceType(), resource.getName(), ex);
        }
    }

    @Override
    public CloudVmInstanceStatus stop(OpenStackContext context, AuthenticatedContext auth, CloudInstance instance) {
        return executeAction(auth, instance, Action.STOP);
    }

    @Override
    public CloudVmInstanceStatus start(OpenStackContext context, AuthenticatedContext auth, CloudInstance instance) {
        return executeAction(auth, instance, Action.START);
    }

    private CloudVmInstanceStatus executeAction(AuthenticatedContext auth, CloudInstance instance, Action action) {
        OSClient<?> osClient = createOSClient(auth);
        ActionResponse actionResponse = osClient.compute().servers().action(instance.getInstanceId(), action);
        if (actionResponse.isSuccess()) {
            return new CloudVmInstanceStatus(instance, InstanceStatus.IN_PROGRESS);
        }
        return new CloudVmInstanceStatus(instance, InstanceStatus.FAILED, actionResponse.getFault());
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.OPENSTACK_INSTANCE;
    }

    @Override
    protected boolean checkStatus(OpenStackContext context, AuthenticatedContext auth, CloudResource resource) {
        Status status = getStatus(auth, resource.getReference());
        if (status != null && context.isBuild()) {
            if (Status.ERROR == status) {
                CloudContext cloudContext = auth.getCloudContext();
                throw new OpenStackResourceException("Instance in failed state", resource.getType(), resource.getName(), cloudContext.getId(),
                        status.name());
            }
            return status == Status.ACTIVE;
        } else {
            return status == null && !context.isBuild();
        }
    }

    private Status getStatus(AuthenticatedContext auth, String serverId) {
        OSClient<?> osClient = createOSClient(auth);
        Server server = osClient.compute().servers().get(serverId);
        Status status = null;
        if (server != null) {
            status = server.getStatus();
        }
        return status;
    }

    private CloudResource getPort(Iterable<CloudResource> computeResources) {
        CloudResource instance = null;
        for (CloudResource computeResource : computeResources) {
            if (computeResource.getType() == ResourceType.OPENSTACK_PORT) {
                instance = computeResource;
            }
        }
        return instance;
    }

    private String getFlavorId(OSClient<?> osClient, String flavorName) {
        List<? extends Flavor> flavors = osClient.compute().flavors().list();
        for (Flavor flavor : flavors) {
            if (flavor.getName().equals(flavorName)) {
                return flavor.getId();
            }
        }
        return null;
    }
}
