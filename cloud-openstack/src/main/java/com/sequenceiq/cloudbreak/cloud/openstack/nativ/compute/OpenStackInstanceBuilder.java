package com.sequenceiq.cloudbreak.cloud.openstack.nativ.compute;

import java.util.Collections;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.OS4JException;
import org.openstack4j.model.compute.Action;
import org.openstack4j.model.compute.ActionResponse;
import org.openstack4j.model.compute.BDMDestType;
import org.openstack4j.model.compute.BDMSourceType;
import org.openstack4j.model.compute.BlockDeviceMappingCreate;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Server;
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
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.OpenStackResourceException;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.context.OpenStackContext;
import com.sequenceiq.cloudbreak.cloud.openstack.status.NovaInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView;
import com.sequenceiq.cloudbreak.cloud.openstack.view.NovaInstanceView;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Service
public class OpenStackInstanceBuilder extends AbstractOpenStackComputeResourceBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackInstanceBuilder.class);

    @Override
    public List<CloudResource> build(OpenStackContext context, long privateId, AuthenticatedContext auth, Group group, Image image,
            List<CloudResource> buildableResource) throws Exception {
        CloudResource resource = buildableResource.get(0);
        try {
            OSClient osClient = createOSClient(auth);
            InstanceTemplate template = getInstanceTemplate(group, privateId);
            CloudResource port = getPort(context.getComputeResources(privateId));
            KeystoneCredentialView osCredential = new KeystoneCredentialView(auth);
            NovaInstanceView novaInstanceView = new NovaInstanceView(template, group.getType());
            String imageId = osClient.images().list(Collections.singletonMap("name", image.getImageName())).get(0).getId();
            ServerCreateBuilder serverCreateBuilder = Builders.server()
                    .name(resource.getName())
                    .image(imageId)
                    .flavor(getFlavorId(osClient, novaInstanceView.getFlavor()))
                    .keypairName(osCredential.getKeyPairName())
                    .addMetadata(novaInstanceView.getMetadataMap())
                    .addNetworkPort(port.getStringParameter(OpenStackConstants.PORT_ID))
                    .userData(new String(Base64.encodeBase64(image.getUserData(group.getType()).getBytes())));
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
            LOGGER.error("Failed to create OpenStack instance with privateId: {}", privateId, ex);
            throw new OpenStackResourceException("Instance creation failed", resourceType(), resource.getName(), ex);
        }
    }

    @Override
    public List<CloudVmInstanceStatus> checkInstances(OpenStackContext context, AuthenticatedContext auth, List<CloudInstance> instances) {
        List<CloudVmInstanceStatus> statuses = Lists.newArrayList();
        OSClient osClient = createOSClient(auth);
        for (CloudInstance instance : instances) {
            Server server = osClient.compute().servers().get(instance.getStringParameter(OpenStackConstants.INSTANCE_ID));
            if (server == null) {
                statuses.add(new CloudVmInstanceStatus(instance, InstanceStatus.TERMINATED));
            } else {
                statuses.add(new CloudVmInstanceStatus(instance, NovaInstanceStatus.get(server)));
            }
        }
        return statuses;
    }

    @Override
    public CloudResource delete(OpenStackContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
        try {
            OSClient osClient = createOSClient(auth);
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
        OSClient osClient = createOSClient(auth);
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
        Server.Status status = getStatus(auth, resource.getReference());
        if (status != null && context.isBuild()) {
            if (Server.Status.ERROR == status) {
                CloudContext cloudContext = auth.getCloudContext();
                throw new OpenStackResourceException("Instance in failed state", resource.getType(), resource.getName(), cloudContext.getId(),
                        status.name());
            }
            return status == Server.Status.ACTIVE;
        } else if (status == null && !context.isBuild()) {
            return true;
        }
        return false;
    }

    private Server.Status getStatus(AuthenticatedContext auth, String serverId) {
        OSClient osClient = createOSClient(auth);
        Server server = osClient.compute().servers().get(serverId);
        Server.Status status = null;
        if (server != null) {
            status = server.getStatus();
        }
        return status;
    }

    private CloudResource getPort(List<CloudResource> computeResources) {
        CloudResource instance = null;
        for (CloudResource computeResource : computeResources) {
            if (computeResource.getType() == ResourceType.OPENSTACK_PORT) {
                instance = computeResource;
            }
        }
        return instance;
    }

    private String getFlavorId(OSClient osClient, String flavorName) {
        List<? extends Flavor> flavors = osClient.compute().flavors().list();
        for (Flavor flavor : flavors) {
            if (flavor.getName().equals(flavorName)) {
                return flavor.getId();
            }
        }
        return null;
    }
}
