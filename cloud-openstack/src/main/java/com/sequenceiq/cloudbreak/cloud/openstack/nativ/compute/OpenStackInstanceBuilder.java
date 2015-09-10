package com.sequenceiq.cloudbreak.cloud.openstack.nativ.compute;

import java.util.Collections;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.OS4JException;
import org.openstack4j.model.compute.ActionResponse;
import org.openstack4j.model.compute.BlockDeviceMappingCreate;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.model.compute.builder.BlockDeviceMappingBuilder;
import org.openstack4j.model.compute.builder.ServerCreateBuilder;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.openstack.OpenStackConstants;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.OpenStackResourceException;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.context.OpenStackContext;
import com.sequenceiq.cloudbreak.cloud.openstack.status.NovaInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView;
import com.sequenceiq.cloudbreak.cloud.openstack.view.NovaInstanceView;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.domain.ResourceType;

@Service
public class OpenStackInstanceBuilder extends AbstractOpenStackComputeResourceBuilder implements ComputeResourceBuilder<OpenStackContext> {
    @Override
    public List<CloudResource> build(OpenStackContext context, long privateId, AuthenticatedContext auth, Group group, Image image,
            List<CloudResource> buildableResource) throws Exception {
        CloudResource resource = buildableResource.get(0);
        try {
            OSClient osClient = createOSClient(auth);
            InstanceTemplate template = getInstanceTemplate(group, privateId);
            CloudResource port = getPort(context.getComputeResources(privateId));
            KeystoneCredentialView osCredential = new KeystoneCredentialView(auth.getCloudCredential());
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
                    .sourceType("image")
                    .deviceName("/dev/vda")
                    .bootIndex(0)
                    .deleteOnTermination(true)
                    .destinationType("local");
            serverCreateBuilder = serverCreateBuilder.blockDevice(blockDeviceMappingBuilder.build());
            for (CloudResource computeResource : context.getComputeResources(privateId)) {
                if (computeResource.getType() == ResourceType.OPENSTACK_ATTACHED_DISK) {
                    BlockDeviceMappingCreate blockDeviceMappingCreate = Builders.blockDeviceMapping()
                            .uuid(computeResource.getReference())
                            .deviceName(computeResource.getStringParameter(OpenStackConstants.VOLUME_MOUNT_POINT))
//                            .deleteOnTermination(true)
                            .sourceType("volume")
                            .destinationType("volume")
                            .build();
                    serverCreateBuilder.blockDevice(blockDeviceMappingCreate);
                }
            }
            ServerCreate serverCreate = serverCreateBuilder.build();
            Server server = osClient.compute().servers().boot(serverCreate);
            return Collections.singletonList(createPersistedResource(resource, server.getId(),
                    Collections.<String, Object>singletonMap(OpenStackConstants.SERVER, server)));
        } catch (OS4JException ex) {
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
    public ResourceType resourceType() {
        return ResourceType.OPENSTACK_INSTANCE;
    }

    @Override
    public int order() {
        return 1;
    }

    @Override
    protected boolean checkStatus(OpenStackContext context, AuthenticatedContext auth, CloudResource resource) {
        OSClient osClient = createOSClient(auth);
        CloudContext cloudContext = auth.getCloudContext();
        Server server = osClient.compute().servers().get(resource.getReference());
        if (server != null && context.isBuild()) {
            Server.Status status = server.getStatus();
            if (Server.Status.ERROR == status) {
                throw new OpenStackResourceException("Instance in failed state", resource.getType(), resource.getName(), cloudContext.getStackId(),
                        status.name());
            }
            return status == Server.Status.ACTIVE;
        } else if (server == null && !context.isBuild()) {
            return true;
        }
        return false;
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
