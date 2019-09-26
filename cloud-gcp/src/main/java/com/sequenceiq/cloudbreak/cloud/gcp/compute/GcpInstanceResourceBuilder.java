package com.sequenceiq.cloudbreak.cloud.gcp.compute;

import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getCustomNetworkId;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getSharedProjectId;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getSubnetId;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import com.google.api.services.compute.model.CustomerEncryptionKey;
import com.google.api.services.compute.model.CustomerEncryptionKeyProtectedDisk;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstancesStartWithEncryptionKeyRequest;
import com.google.api.services.compute.model.Metadata;
import com.google.api.services.compute.model.Metadata.Items;
import com.google.api.services.compute.model.NetworkInterface;
import com.google.api.services.compute.model.Operation;
import com.google.api.services.compute.model.Scheduling;
import com.google.api.services.compute.model.ServiceAccount;
import com.google.api.services.compute.model.Tags;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpDiskEncryptionService;
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
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes.Volume;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudGcsView;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.service.DefaultCostTaggingService;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;

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

    @Inject
    private GcpDiskEncryptionService gcpDiskEncryptionService;

    @Inject
    private PersistenceNotifier persistenceNotifier;

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
        Compute compute = context.getCompute();

        List<CloudResource> computeResources = context.getComputeResources(privateId);
        List<AttachedDisk> listOfDisks = new ArrayList<>();

        listOfDisks.addAll(getBootDiskList(computeResources, projectId, location.getAvailabilityZone()));
        listOfDisks.addAll(getAttachedDisks(computeResources, projectId));

        listOfDisks.forEach(disk -> gcpDiskEncryptionService.addEncryptionKeyToDisk(template, disk));

        Instance instance = new Instance();
        instance.setMachineType(String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/machineTypes/%s",
                projectId, location.getAvailabilityZone().value(), template.getFlavor()));
        instance.setName(buildableResource.get(0).getName());
        instance.setCanIpForward(Boolean.TRUE);
        instance.setNetworkInterfaces(getNetworkInterface(context, computeResources, group, cloudStack));
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
            verifyOperation(operation, buildableResource);
            updateDiskSetWithInstanceName(auth, computeResources, instance);
            return singletonList(createOperationAwareCloudResource(buildableResource.get(0), operation));
        } catch (GoogleJsonResponseException e) {
            throw new GcpResourceException(checkException(e), resourceType(), buildableResource.get(0).getName());
        }
    }

    private void verifyOperation(Operation operation, List<CloudResource> buildableResource) {
        if (operation.getHttpErrorStatusCode() != null) {
            throw new GcpResourceException(operation.getHttpErrorMessage(), resourceType(), buildableResource.get(0).getName());
        }
        if (operation.getError() != null && !operation.getError().isEmpty()) {
            throw new GcpResourceException(operation.getError().getErrors().stream()
                    .map(Operation.Error.Errors::getMessage).collect(Collectors.joining(",")), resourceType(), buildableResource.get(0).getName());
        }
    }

    private void updateDiskSetWithInstanceName(AuthenticatedContext auth, List<CloudResource> computeResources, Instance instance) {
        for (CloudResource resource : filterResourcesByType(computeResources, ResourceType.GCP_ATTACHED_DISKSET)) {
            resource.setInstanceId(instance.getName());
            persistenceNotifier.notifyUpdate(resource, auth.getCloudContext());
        }
    }

    private List<ServiceAccount> extractServiceAccounts(CloudStack cloudStack) {
        if (noFileSystemIsConfigured(cloudStack)) {
            return null;
        }
        List<CloudFileSystemView> cloudFileSystems = cloudStack.getFileSystem().get().getCloudFileSystems();
        if (cloudFileSystems.size() > 1) {
            throw new CloudConnectorException("Multiple file systems (identities) are not yet supported on GCP!");
        }
        CloudGcsView cloudFileSystem = (CloudGcsView) cloudFileSystems.get(0);
        String email = cloudFileSystem.getServiceAccountEmail();
        return StringUtils.isEmpty(email) ? null
                : singletonList(new ServiceAccount()
                .setEmail(email)
                .setScopes(singletonList("https://www.googleapis.com/auth/cloud-platform")));
    }

    private boolean noFileSystemIsConfigured(CloudStack cloudStack) {
        return cloudStack.getFileSystem().isEmpty() || cloudStack.getFileSystem().get().getCloudFileSystems().isEmpty();
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
        List<CloudVmInstanceStatus> result = new ArrayList<>();
        for (CloudInstance instance : instances) {
            try {
                LOGGER.info("Checking instance: {}", instance);
                Operation operation = check(context, instance.getStringParameter(OPERATION_ID));
                boolean finished = operation == null || GcpStackUtil.isOperationFinished(operation);
                InstanceStatus status = finished ? context.isBuild() ? InstanceStatus.STARTED : InstanceStatus.STOPPED : InstanceStatus.IN_PROGRESS;
                LOGGER.info("Instance: {} status: {}", instances, status);
                result.add(new CloudVmInstanceStatus(instance, status));
            } catch (Exception ignored) {
                LOGGER.info("Failed to check instance state of {}", instance);
                result.add(new CloudVmInstanceStatus(instance, InstanceStatus.IN_PROGRESS));
            }
        }
        return result;
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
            listOfDisks.add(createDisk(projectId, true, resource.getName(), zone.value(), true));
        }
        return listOfDisks;
    }

    private Collection<AttachedDisk> getAttachedDisks(Iterable<CloudResource> resources, String projectId) {
        Collection<AttachedDisk> listOfDisks = new ArrayList<>();
        for (CloudResource resource : filterResourcesByType(resources, ResourceType.GCP_ATTACHED_DISKSET)) {
            VolumeSetAttributes volumeSetAttributes = resource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
            for (Volume volume : volumeSetAttributes.getVolumes()) {
                listOfDisks.add(createDisk(projectId, false, volume.getId(), volumeSetAttributes.getAvailabilityZone(), Boolean.FALSE));
            }
        }
        return listOfDisks;
    }

    private AttachedDisk createDisk(String projectId, boolean boot, String resourceName, String zone, boolean autoDelete) {
        AttachedDisk attachedDisk = new AttachedDisk();
        attachedDisk.setBoot(boot);
        attachedDisk.setAutoDelete(autoDelete);
        attachedDisk.setType(GCP_DISK_TYPE);
        attachedDisk.setMode(GCP_DISK_MODE);
        attachedDisk.setDeviceName(resourceName);
        attachedDisk.setSource(String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/disks/%s",
                projectId, zone, resourceName));
        return attachedDisk;
    }

    private List<NetworkInterface> getNetworkInterface(GcpContext context, Iterable<CloudResource> computeResources, Group group, CloudStack stack)
            throws IOException {
        boolean noPublicIp = context.getNoPublicIp();
        String projectId = context.getProjectId();
        Location location = context.getLocation();
        Compute compute = context.getCompute();

        NetworkInterface networkInterface = new NetworkInterface();
        List<CloudResource> subnet = filterResourcesByType(context.getNetworkResources(), ResourceType.GCP_SUBNET);
        String networkName = subnet.isEmpty() ? filterResourcesByType(context.getNetworkResources(),
                ResourceType.GCP_NETWORK).get(0).getName() : subnet.get(0).getName();
        networkInterface.setName(networkName);

        if (!noPublicIp) {
            AccessConfig accessConfig = new AccessConfig();
            accessConfig.setName(networkName);
            accessConfig.setType("ONE_TO_ONE_NAT");
            List<CloudResource> reservedIp = filterResourcesByType(computeResources, ResourceType.GCP_RESERVED_IP);
            if (InstanceGroupType.GATEWAY == group.getType() && !reservedIp.isEmpty()) {
                Addresses.Get getReservedIp = compute.addresses().get(projectId, location.getRegion().value(), reservedIp.get(0).getName());
                accessConfig.setNatIP(getReservedIp.execute().getAddress());
            }
            networkInterface.setAccessConfigs(singletonList(accessConfig));
        }
        prepareNetworkAndSubnet(projectId, location.getRegion(), stack.getNetwork(), networkInterface, subnet, networkName);
        return singletonList(networkInterface);
    }

    private void prepareNetworkAndSubnet(String projectId, Region region, Network network, NetworkInterface networkInterface,
            List<CloudResource> subnet, String networkName) {
        if (isNotEmpty(getSharedProjectId(network))) {
            networkInterface.setNetwork(getNetworkUrl(getSharedProjectId(network), getCustomNetworkId(network)));
            networkInterface.setSubnetwork(getSubnetUrl(getSharedProjectId(network), region.value(), getSubnetId(network)));
        } else {
            if (subnet.isEmpty()) {
                networkInterface.setNetwork(getNetworkUrl(projectId, networkName));
            } else {
                networkInterface.setSubnetwork(getSubnetUrl(projectId, region.value(), networkName));
            }
        }
    }

    private String getSubnetUrl(String sharedProjectId, String value, String customSubnetworkId) {
        return String.format("https://www.googleapis.com/compute/v1/projects/%s/regions/%s/subnetworks/%s", sharedProjectId, value, customSubnetworkId);
    }

    private String getNetworkUrl(String sharedProjectId, String customNetworkId) {
        return String.format("https://www.googleapis.com/compute/v1/projects/%s/global/networks/%s", sharedProjectId, customNetworkId);
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
            Instance instanceResponse = get.execute();
            if (state.equals(instanceResponse.getStatus())) {
                Operation operation = stopRequest ? compute.instances().stop(projectId, availabilityZone, instanceId).setPrettyPrint(true).execute()
                        : executeStartOperation(projectId, availabilityZone, compute, instanceId, instance.getTemplate(), instanceResponse.getDisks());
                CloudInstance operationAwareCloudInstance = createOperationAwareCloudInstance(instance, operation);
                return checkInstances(context, auth, singletonList(operationAwareCloudInstance)).get(0);
            } else {
                LOGGER.debug("Instance {} is not in {} state - won't start it.", state, instanceId);
                return null;
            }
        } catch (IOException e) {
            throw new GcpResourceException(String.format("An error occurred while stopping the vm '%s'", instanceId), e);
        }
    }

    private Operation executeStartOperation(String projectId, String availabilityZone, Compute compute, String instanceId, InstanceTemplate template,
            List<AttachedDisk> disks) throws IOException {

        if (gcpDiskEncryptionService.hasCustomEncryptionRequested(template)) {
            CustomerEncryptionKey customerEncryptionKey = gcpDiskEncryptionService.createCustomerEncryptionKey(template);
            List<CustomerEncryptionKeyProtectedDisk> protectedDisks = disks
                    .stream()
                    .map(AttachedDisk::getSource)
                    .map(toCustomerEncryptionKeyProtectedDisk(customerEncryptionKey))
                    .collect(Collectors.toList());
            InstancesStartWithEncryptionKeyRequest request = new InstancesStartWithEncryptionKeyRequest();
            request.setDisks(protectedDisks);
            return compute.instances().startWithEncryptionKey(projectId, availabilityZone, instanceId, request).setPrettyPrint(true).execute();
        } else {
            return compute.instances().start(projectId, availabilityZone, instanceId).setPrettyPrint(true).execute();
        }
    }

    private Function<String, CustomerEncryptionKeyProtectedDisk> toCustomerEncryptionKeyProtectedDisk(CustomerEncryptionKey diskEncryptionKey) {
        return source -> {
            CustomerEncryptionKeyProtectedDisk protectedDisk = new CustomerEncryptionKeyProtectedDisk();
            protectedDisk.setDiskEncryptionKey(diskEncryptionKey);
            protectedDisk.setSource(source);
            return protectedDisk;
        };
    }

}
