package com.sequenceiq.cloudbreak.cloud.gcp.compute;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.AccessConfig;
import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.Metadata;
import com.google.api.services.compute.model.NetworkInterface;
import com.google.api.services.compute.model.Operation;
import com.google.api.services.compute.model.Tags;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Component
public class GcpInstanceResourceBuilder extends AbstractGcpComputeBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpInstanceResourceBuilder.class);
    private static final String GCP_DISK_TYPE = "PERSISTENT";
    private static final String GCP_DISK_MODE = "READ_WRITE";

    @Override
    public List<CloudResource> create(GcpContext context, long privateId, AuthenticatedContext auth, Group group, Image image) {
        CloudContext cloudContext = auth.getCloudContext();
        String resourceName = getResourceNameService().resourceName(resourceType(), cloudContext.getName(), group.getName(), privateId);
        return Arrays.asList(createNamedResource(resourceType(), resourceName));
    }

    @Override
    public List<CloudResource> build(GcpContext context, long privateId, AuthenticatedContext auth, Group group, Image image,
            List<CloudResource> buildableResource) throws Exception {
        InstanceTemplate template = group.getInstances().get(0).getTemplate();
        String projectId = context.getProjectId();
        Location location = context.getLocation();

        Compute compute = context.getCompute();

        List<CloudResource> computeResources = context.getComputeResources(privateId);
        List<AttachedDisk> listOfDisks = new ArrayList<>();
        listOfDisks.addAll(getBootDiskList(computeResources, projectId, location.getAvailabilityZone()));
        listOfDisks.addAll(getAttachedDisks(computeResources, projectId, location.getAvailabilityZone()));

        Instance instance = new Instance();
        instance.setMachineType(String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/machineTypes/%s",
                projectId, location.getAvailabilityZone().value(), template.getFlavor()));
        instance.setName(buildableResource.get(0).getName());
        instance.setCanIpForward(Boolean.TRUE);
        instance.setNetworkInterfaces(getNetworkInterface(context.getNetworkResources(), location.getRegion(), group, compute, projectId));
        instance.setDisks(listOfDisks);

        Tags tags = new Tags();
        List<String> tagList = new ArrayList<>();
        tagList.add(group.getName().toLowerCase().replaceAll("[^A-Za-z0-9 ]", ""));
        tagList.add(GcpStackUtil.getClusterTag(auth.getCloudContext()));
        tags.setItems(tagList);
        instance.setTags(tags);

        Metadata metadata = new Metadata();
        metadata.setItems(new ArrayList<Metadata.Items>());

        Metadata.Items sshMetaData = new Metadata.Items();
        sshMetaData.setKey("sshKeys");
        sshMetaData.setValue(auth.getCloudCredential().getLoginUserName() + ":" + auth.getCloudCredential().getPublicKey());

        Metadata.Items startupScript = new Metadata.Items();
        startupScript.setKey("startup-script");
        startupScript.setValue(image.getUserData(group.getType()));

        metadata.getItems().add(sshMetaData);
        metadata.getItems().add(startupScript);
        instance.setMetadata(metadata);

        Compute.Instances.Insert insert = compute.instances().insert(projectId, location.getAvailabilityZone().value(), instance);
        insert.setPrettyPrint(Boolean.TRUE);
        try {
            Operation operation = insert.execute();
            if (operation.getHttpErrorStatusCode() != null) {
                throw new GcpResourceException(operation.getHttpErrorMessage(), resourceType(), buildableResource.get(0).getName());
            }
            return asList(createOperationAwareCloudResource(buildableResource.get(0), operation));
        } catch (GoogleJsonResponseException e) {
            throw new GcpResourceException(checkException(e), resourceType(), buildableResource.get(0).getName());
        }
    }

    @Override
    public CloudResource delete(GcpContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
        String resourceName = resource.getName();
        try {
            Operation operation = context.getCompute().instances()
                    .delete(context.getProjectId(), context.getLocation().getAvailabilityZone().value(), resourceName).execute();
            return createOperationAwareCloudResource(resource, operation);
        } catch (GoogleJsonResponseException e) {
            exceptionHandler(e, resourceName, resourceType());
        }
        return null;
    }

    @Override
    public List<CloudVmInstanceStatus> checkInstances(GcpContext context, AuthenticatedContext auth, List<CloudInstance> instances) {
        CloudInstance cloudInstance = instances.get(0);
        try {
            LOGGER.info("Checking instance: {}", cloudInstance);
            Operation operation = check(context, cloudInstance);
            boolean finished = operation == null || GcpStackUtil.analyzeOperation(operation);
            InstanceStatus status = finished ? context.isBuild() ? InstanceStatus.STARTED : InstanceStatus.STOPPED : InstanceStatus.IN_PROGRESS;
            LOGGER.info("Instance: {} status: {}", instances, status);
            return asList(new CloudVmInstanceStatus(cloudInstance, status));
        } catch (Exception e) {
            LOGGER.info("Failed to check instance state of {}", cloudInstance);
            return asList(new CloudVmInstanceStatus(cloudInstance, InstanceStatus.IN_PROGRESS));
        }
    }

    @Override
    public CloudVmInstanceStatus stop(GcpContext context, AuthenticatedContext auth, CloudInstance instance) {
        return stopStart(context, auth, instance, true);
    }

    @Override
    public CloudVmInstanceStatus start(GcpContext context, AuthenticatedContext auth, CloudInstance instance) {
        return stopStart(context, auth, instance, false);
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCP_INSTANCE;
    }

    @Override
    public int order() {
        return 2;
    }

    private List<AttachedDisk> getBootDiskList(List<CloudResource> resources, String projectId, AvailabilityZone zone) {
        List<AttachedDisk> listOfDisks = new ArrayList<>();
        for (CloudResource resource : filterResourcesByType(resources, ResourceType.GCP_DISK)) {
            listOfDisks.add(createDisk(resource, projectId, zone, true));
        }
        return listOfDisks;
    }

    private List<AttachedDisk> getAttachedDisks(List<CloudResource> resources, String projectId, AvailabilityZone zone) {
        List<AttachedDisk> listOfDisks = new ArrayList<>();
        for (CloudResource resource : filterResourcesByType(resources, ResourceType.GCP_ATTACHED_DISK)) {
            listOfDisks.add(createDisk(resource, projectId, zone, false));
        }
        return listOfDisks;
    }

    private AttachedDisk createDisk(CloudResource resource, String projectId, AvailabilityZone zone, boolean boot) {
        AttachedDisk attachedDisk = new AttachedDisk();
        attachedDisk.setBoot(boot);
        attachedDisk.setAutoDelete(true);
        attachedDisk.setType(GCP_DISK_TYPE);
        attachedDisk.setMode(GCP_DISK_MODE);
        attachedDisk.setDeviceName(resource.getName());
        attachedDisk.setSource(String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/disks/%s",
                projectId, zone.value(), resource.getName()));
        return attachedDisk;
    }

    private List<NetworkInterface> getNetworkInterface(List<CloudResource> resources,
            Region region, Group group, Compute compute, String projectId) throws IOException {
        NetworkInterface networkInterface = new NetworkInterface();
        String networkName = filterResourcesByType(resources, ResourceType.GCP_NETWORK).get(0).getName();
        networkInterface.setName(networkName);
        AccessConfig accessConfig = new AccessConfig();
        accessConfig.setName(networkName);
        accessConfig.setType("ONE_TO_ONE_NAT");
        if (InstanceGroupType.GATEWAY == group.getType()) {
            Compute.Addresses.Get getReservedIp = compute.addresses().get(projectId, region.value(),
                    filterResourcesByType(resources, ResourceType.GCP_RESERVED_IP).get(0).getName());
            accessConfig.setNatIP(getReservedIp.execute().getAddress());
        }
        networkInterface.setAccessConfigs(asList(accessConfig));
        networkInterface.setNetwork(String.format("https://www.googleapis.com/compute/v1/projects/%s/global/networks/%s", projectId, networkName));
        return asList(networkInterface);
    }

    private List<CloudResource> filterResourcesByType(Collection<CloudResource> resources, ResourceType resourceType) {
        List<CloudResource> resourcesTemp = new ArrayList<>();
        for (CloudResource resource : resources) {
            if (resourceType.equals(resource.getType())) {
                resourcesTemp.add(resource);
            }
        }
        return resourcesTemp;
    }

    private CloudVmInstanceStatus stopStart(GcpContext context, AuthenticatedContext auth, CloudInstance instance, boolean stopRequest) {
        String projectId = GcpStackUtil.getProjectId(auth.getCloudCredential());
        String availabilityZone = context.getLocation().getAvailabilityZone().value();
        Compute compute = context.getCompute();
        String instanceId = instance.getInstanceId();
        try {
            Compute.Instances.Get get = compute.instances().get(projectId, availabilityZone, instanceId);
            String state = stopRequest ? "RUNNING" : "TERMINATED";
            if (state.equals(get.execute().getStatus())) {
                Operation operation = stopRequest ? compute.instances().stop(projectId, availabilityZone, instanceId).setPrettyPrint(true).execute()
                        : compute.instances().start(projectId, availabilityZone, instanceId).setPrettyPrint(true).execute();
                CloudInstance operationAwareCloudInstance = createOperationAwareCloudInstance(instance, operation);
                return checkInstances(context, auth, asList(operationAwareCloudInstance)).get(0);
            } else {
                LOGGER.info("Instance {} is not in {} state - won't start it.", state, instanceId);
                return null;
            }
        } catch (IOException e) {
            throw new GcpResourceException(String.format("An error occurred while stopping the vm '%s'", instanceId), e);
        }
    }

}
