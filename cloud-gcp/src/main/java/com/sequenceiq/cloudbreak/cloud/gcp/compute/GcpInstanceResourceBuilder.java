package com.sequenceiq.cloudbreak.cloud.gcp.compute;

import static java.util.Collections.singletonList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.Addresses;
import com.google.api.services.compute.Compute.Instances.Get;
import com.google.api.services.compute.Compute.Instances.Insert;
import com.google.api.services.compute.model.AccessConfig;
import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.Metadata;
import com.google.api.services.compute.model.Metadata.Items;
import com.google.api.services.compute.model.NetworkInterface;
import com.google.api.services.compute.model.Operation;
import com.google.api.services.compute.model.Scheduling;
import com.google.api.services.compute.model.ServiceAccount;
import com.google.api.services.compute.model.Tags;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudGcsView;
import com.sequenceiq.cloudbreak.common.service.DefaultCostTaggingService;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Component
public class GcpInstanceResourceBuilder extends AbstractGcpComputeBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpInstanceResourceBuilder.class);

    private static final String SERVICE_ACCOUNT_EMAIL = "serviceAccountEmail";

    private static final String GCP_DISK_TYPE = "PERSISTENT";

    private static final String GCP_DISK_MODE = "READ_WRITE";

    private static final String PREEMPTIBLE = "preemptible";

    private static final int ORDER = 3;

    @Inject
    private DefaultCostTaggingService defaultCostTaggingService;

    @Override
    public List<CloudResource> create(GcpContext context, long privateId, AuthenticatedContext auth, Group group, Image image) {
        CloudContext cloudContext = auth.getCloudContext();
        String resourceName = getResourceNameService().resourceName(resourceType(), cloudContext.getName(), group.getName(), privateId);
        return singletonList(createNamedResource(resourceType(), resourceName));
    }

    @Override
    public List<CloudResource> build(GcpContext context, long privateId, AuthenticatedContext auth, Group group, List<CloudResource> buildableResource,
            CloudStack cloudStack) throws Exception {
        InstanceTemplate template = group.getReferenceInstanceConfiguration().getTemplate();
        String projectId = context.getProjectId();
        Location location = context.getLocation();
        boolean noPublicIp = context.getNoPublicIp();

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
        instance.setNetworkInterfaces(getNetworkInterface(context.getNetworkResources(), computeResources, location.getRegion(), group, compute, projectId,
                noPublicIp));
        instance.setDisks(listOfDisks);
        instance.setServiceAccounts(extractServiceAccounts(cloudStack));
        Scheduling scheduling = new Scheduling();
        boolean preemptible = false;
        if (template.getParameter(PREEMPTIBLE, Boolean.class) != null) {
            preemptible = template.getParameter(PREEMPTIBLE, Boolean.class);
        }
        scheduling.setPreemptible(preemptible);
        instance.setScheduling(scheduling);

        Tags tags = new Tags();
        List<String> tagList = new ArrayList<>();
        Map<String, String> labels = new HashMap<>();
        String groupname = group.getName().toLowerCase().replaceAll("[^A-Za-z0-9 ]", "");
        tagList.add(groupname);
        Map<String, String> instanceTag = defaultCostTaggingService.prepareInstanceTagging();
        for (Entry<String, String> entry : instanceTag.entrySet()) {
            tagList.add(String.format("%s-%s", entry.getKey(), entry.getValue()));
            labels.put(entry.getKey(), entry.getValue());
        }

        tagList.add(GcpStackUtil.getClusterTag(auth.getCloudContext()));
        tagList.add(GcpStackUtil.getGroupClusterTag(auth.getCloudContext(), group));
        cloudStack.getTags().forEach((key, value) -> tagList.add(key + '-' + value));

        labels.putAll(cloudStack.getTags());
        tags.setItems(tagList);

        instance.setTags(tags);
        instance.setLabels(labels);

        Metadata metadata = new Metadata();
        metadata.setItems(new ArrayList<>());

        Items sshMetaData = new Items();
        sshMetaData.setKey("ssh-keys");
        sshMetaData.setValue(group.getInstanceAuthentication().getLoginUserName() + ':' + group.getInstanceAuthentication().getPublicKey());

        Items blockProjectWideSsh = new Items();
        blockProjectWideSsh.setKey("block-project-ssh-keys");
        blockProjectWideSsh.setValue("TRUE");

        Items startupScript = new Items();
        startupScript.setKey("startup-script");
        startupScript.setValue(cloudStack.getImage().getUserDataByType(group.getType()));

        metadata.getItems().add(sshMetaData);
        metadata.getItems().add(startupScript);
        metadata.getItems().add(blockProjectWideSsh);
        instance.setMetadata(metadata);

        Insert insert = compute.instances().insert(projectId, location.getAvailabilityZone().value(), instance);
        insert.setPrettyPrint(Boolean.TRUE);
        try {
            Operation operation = insert.execute();
            if (operation.getHttpErrorStatusCode() != null) {
                throw new GcpResourceException(operation.getHttpErrorMessage(), resourceType(), buildableResource.get(0).getName());
            }
            return singletonList(createOperationAwareCloudResource(buildableResource.get(0), operation));
        } catch (GoogleJsonResponseException e) {
            throw new GcpResourceException(checkException(e), resourceType(), buildableResource.get(0).getName());
        }
    }

    private List<ServiceAccount> extractServiceAccounts(CloudStack cloudStack) {
        if (!cloudStack.getFileSystem().isPresent()) {
            return null;
        }
        CloudGcsView cloudFileSystem = (CloudGcsView) cloudStack.getFileSystem().get().getCloudFileSystem();
        String email = cloudFileSystem.getServiceAccountEmail();
        return StringUtils.isEmpty(email) ? null : singletonList(new ServiceAccount()
                .setEmail(email)
                .setScopes(singletonList("https://www.googleapis.com/auth/cloud-platform")));
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
            boolean finished = operation == null || GcpStackUtil.isOperationFinished(operation);
            InstanceStatus status = finished ? context.isBuild() ? InstanceStatus.STARTED : InstanceStatus.STOPPED : InstanceStatus.IN_PROGRESS;
            LOGGER.info("Instance: {} status: {}", instances, status);
            return singletonList(new CloudVmInstanceStatus(cloudInstance, status));
        } catch (Exception ignored) {
            LOGGER.info("Failed to check instance state of {}", cloudInstance);
            return singletonList(new CloudVmInstanceStatus(cloudInstance, InstanceStatus.IN_PROGRESS));
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
        return ORDER;
    }

    private Collection<AttachedDisk> getBootDiskList(Iterable<CloudResource> resources, String projectId, AvailabilityZone zone) {
        Collection<AttachedDisk> listOfDisks = new ArrayList<>();
        for (CloudResource resource : filterResourcesByType(resources, ResourceType.GCP_DISK)) {
            listOfDisks.add(createDisk(resource, projectId, zone, true));
        }
        return listOfDisks;
    }

    private Collection<AttachedDisk> getAttachedDisks(Iterable<CloudResource> resources, String projectId, AvailabilityZone zone) {
        Collection<AttachedDisk> listOfDisks = new ArrayList<>();
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

    private List<NetworkInterface> getNetworkInterface(Iterable<CloudResource> networkResources, Iterable<CloudResource> computeResources,
            Region region, Group group, Compute compute, String projectId, boolean noPublicIp) throws IOException {
        NetworkInterface networkInterface = new NetworkInterface();
        List<CloudResource> subnet = filterResourcesByType(networkResources, ResourceType.GCP_SUBNET);
        String networkName = subnet.isEmpty() ? filterResourcesByType(networkResources, ResourceType.GCP_NETWORK).get(0).getName() : subnet.get(0).getName();
        networkInterface.setName(networkName);

        if (!noPublicIp) {
            AccessConfig accessConfig = new AccessConfig();
            accessConfig.setName(networkName);
            accessConfig.setType("ONE_TO_ONE_NAT");
            List<CloudResource> reservedIp = filterResourcesByType(computeResources, ResourceType.GCP_RESERVED_IP);
            if (InstanceGroupType.GATEWAY == group.getType() && !reservedIp.isEmpty()) {
                Addresses.Get getReservedIp = compute.addresses().get(projectId, region.value(), reservedIp.get(0).getName());
                accessConfig.setNatIP(getReservedIp.execute().getAddress());
            }
            networkInterface.setAccessConfigs(singletonList(accessConfig));
        }

        if (subnet.isEmpty()) {
            networkInterface.setNetwork(
                    String.format("https://www.googleapis.com/compute/v1/projects/%s/global/networks/%s", projectId, networkName));
        } else {
            networkInterface.setSubnetwork(
                    String.format("https://www.googleapis.com/compute/v1/projects/%s/regions/%s/subnetworks/%s", projectId, region.value(), networkName));
        }
        return singletonList(networkInterface);
    }

    private List<CloudResource> filterResourcesByType(Iterable<CloudResource> resources, ResourceType resourceType) {
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
            Get get = compute.instances().get(projectId, availabilityZone, instanceId);
            String state = stopRequest ? "RUNNING" : "TERMINATED";
            if (state.equals(get.execute().getStatus())) {
                Operation operation = stopRequest ? compute.instances().stop(projectId, availabilityZone, instanceId).setPrettyPrint(true).execute()
                        : compute.instances().start(projectId, availabilityZone, instanceId).setPrettyPrint(true).execute();
                CloudInstance operationAwareCloudInstance = createOperationAwareCloudInstance(instance, operation);
                return checkInstances(context, auth, singletonList(operationAwareCloudInstance)).get(0);
            } else {
                LOGGER.info("Instance {} is not in {} state - won't start it.", state, instanceId);
                return null;
            }
        } catch (IOException e) {
            throw new GcpResourceException(String.format("An error occurred while stopping the vm '%s'", instanceId), e);
        }
    }

}
