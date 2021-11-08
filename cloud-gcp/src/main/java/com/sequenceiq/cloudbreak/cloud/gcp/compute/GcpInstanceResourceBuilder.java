package com.sequenceiq.cloudbreak.cloud.gcp.compute;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.CLOUD_STACK_TYPE_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.FREEIPA_STACK_TYPE;
import static java.util.Collections.singletonList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.Addresses;
import com.google.api.services.compute.Compute.InstanceGroups.AddInstances;
import com.google.api.services.compute.Compute.Instances.Get;
import com.google.api.services.compute.Compute.Instances.Insert;
import com.google.api.services.compute.model.AccessConfig;
import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.CustomerEncryptionKey;
import com.google.api.services.compute.model.CustomerEncryptionKeyProtectedDisk;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceGroupList;
import com.google.api.services.compute.model.InstanceGroupsAddInstancesRequest;
import com.google.api.services.compute.model.InstanceReference;
import com.google.api.services.compute.model.InstancesStartWithEncryptionKeyRequest;
import com.google.api.services.compute.model.Metadata;
import com.google.api.services.compute.model.Metadata.Items;
import com.google.api.services.compute.model.NetworkInterface;
import com.google.api.services.compute.model.Operation;
import com.google.api.services.compute.model.Scheduling;
import com.google.api.services.compute.model.ServiceAccount;
import com.google.api.services.compute.model.Tags;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpNetworkInterfaceProvider;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.CustomGcpDiskEncryptionCreatorService;
import com.sequenceiq.cloudbreak.cloud.gcp.service.CustomGcpDiskEncryptionService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
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
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class GcpInstanceResourceBuilder extends AbstractGcpComputeBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpInstanceResourceBuilder.class);

    private static final String SERVICE_ACCOUNT_EMAIL = "serviceAccountEmail";

    private static final String GCP_DISK_TYPE = "PERSISTENT";

    private static final String GCP_DISK_MODE = "READ_WRITE";

    private static final String PREEMPTIBLE = "preemptible";

    private static final String GCP_CLOUD_STORAGE_RW_SCOPE = "https://www.googleapis.com/auth/devstorage.read_write";

    private static final int MAX_TAG_LENGTH = 63;

    private static final int ORDER = 3;

    private static final String INSTANCE_REFERENCE_URI = "https://www.googleapis.com/compute/v1/projects/%s/zones/%s/instances/%s";

    @Inject
    private CustomGcpDiskEncryptionCreatorService customGcpDiskEncryptionCreatorService;

    @Inject
    private CustomGcpDiskEncryptionService customGcpDiskEncryptionService;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Inject
    private GcpNetworkInterfaceProvider gcpNetworkInterfaceProvider;

    @Inject
    private GcpStackUtil gcpStackUtil;

    @Inject
    private GcpLabelUtil gcpLabelUtil;

    @Override
    public List<CloudResource> create(GcpContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group, Image image) {
        CloudContext cloudContext = auth.getCloudContext();
        String resourceName = getResourceNameService().resourceName(resourceType(), cloudContext.getName(), group.getName(), privateId);
        return singletonList(createNamedResource(resourceType(), resourceName, instance.getAvailabilityZone()));
    }

    @Override
    public List<CloudResource> build(GcpContext context, CloudInstance cloudInstance, long privateId, AuthenticatedContext auth,
            Group group, List<CloudResource> buildableResource, CloudStack cloudStack) throws Exception {
        InstanceTemplate template = group.getReferenceInstanceTemplate();
        String projectId = context.getProjectId();
        String location = cloudInstance.getAvailabilityZone();
        Compute compute = context.getCompute();

        List<CloudResource> computeResources = context.getComputeResources(privateId);
        List<AttachedDisk> listOfDisks = new ArrayList<>();

        listOfDisks.addAll(getBootDiskList(computeResources, projectId, cloudInstance.getAvailabilityZone()));
        listOfDisks.addAll(getAttachedDisks(computeResources, projectId));

        listOfDisks.forEach(disk -> customGcpDiskEncryptionService.addEncryptionKeyToDisk(template, disk));

        Instance instance = new Instance();
        instance.setMachineType(String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/machineTypes/%s",
                projectId, location, template.getFlavor()));
        instance.setDescription(description());
        instance.setName(buildableResource.get(0).getName());
        Optional<CloudFileSystemView> cloudFileSystemView = group.getIdentity();
        if (cloudFileSystemView.isPresent()) {
            CloudGcsView gcsView = (CloudGcsView) cloudFileSystemView.get();
            ServiceAccount serviceAccount = new ServiceAccount();
            serviceAccount.setEmail(gcsView.getServiceAccountEmail());
            serviceAccount.setScopes(Arrays.asList(GCP_CLOUD_STORAGE_RW_SCOPE));
            instance.setServiceAccounts(Arrays.asList(serviceAccount));
        }
        // For FreeIPA hosts set the hostname during creation to avoid Google Network Manager overriding it with internal hostnames
        if (cloudStack.getParameters() != null
                && cloudStack.getParameters().getOrDefault(CLOUD_STACK_TYPE_PARAMETER, "").equals(FREEIPA_STACK_TYPE)) {
            String hostname = getHostname(group, privateId);
            if (hostname != null) {
                instance.setHostname(hostname);
            }
        }
        instance.setCanIpForward(Boolean.TRUE);
        instance.setNetworkInterfaces(getNetworkInterface(context, computeResources, group, cloudStack, cloudInstance));
        instance.setDisks(listOfDisks);
        instance.setServiceAccounts(extractServiceAccounts(group));
        Scheduling scheduling = new Scheduling();
        boolean preemptible = false;
        if (template.getParameter(PREEMPTIBLE, Boolean.class) != null) {
            preemptible = template.getParameter(PREEMPTIBLE, Boolean.class);
        }
        scheduling.setPreemptible(preemptible);
        instance.setScheduling(scheduling);

        Tags tags = new Tags();
        List<String> tagList = new ArrayList<>();
        String groupname = group.getName().toLowerCase().replaceAll("[^A-Za-z0-9 ]", "");
        addTag(tagList, groupname);
        // GCP firewall rules' target tags need to be added to the network tags for the firewall rule to take effect
        if (group.getSecurity() != null && group.getSecurity().getCloudSecurityId() != null) {
            addTag(tagList, group.getSecurity().getCloudSecurityId());
        }
        addTag(tagList, gcpStackUtil.getClusterTag(auth.getCloudContext()));
        addTag(tagList, gcpStackUtil.getGroupClusterTag(auth.getCloudContext(), group));
        addTag(tagList, gcpStackUtil.getGroupTypeTag(group.getType()));
        Map<String, String> labelsFromTags = gcpLabelUtil.createLabelsFromTags(cloudStack);
        labelsFromTags.forEach((key, value) -> addTag(tagList, mergeAndTrimKV(key, value, '-', MAX_TAG_LENGTH)));

        tags.setItems(tagList);

        instance.setTags(tags);
        instance.setLabels(labelsFromTags);

        Metadata metadata = new Metadata();
        metadata.setItems(new ArrayList<>());

        Items sshMetaData = new Items();
        sshMetaData.setKey("ssh-keys");
        sshMetaData.setValue(getPublicKey(group.getPublicKey(), group.getLoginUserName()));

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

        Insert insert = compute.instances().insert(projectId, cloudInstance.getAvailabilityZone(), instance);
        insert.setPrettyPrint(Boolean.TRUE);
        try {
            Operation operation = insert.execute();
            verifyOperation(operation, buildableResource);
            updateDiskSetWithInstanceName(auth, computeResources, instance);
            assignToExistingInstanceGroup(context, group, instance, buildableResource);
            return singletonList(createOperationAwareCloudResource(buildableResource.get(0), operation));
        } catch (GoogleJsonResponseException e) {
            throw new GcpResourceException(checkException(e), resourceType(), buildableResource.get(0).getName());
        }
    }

    /**
     * if a InstanceGroup was created in GCP for this Instance's group, then after creating this compute instance assign it to that group.
     * the group in general can be used to manage all instances in the same group, specifiaclly one way is used to assign to a load balancer.
     * also provides aggrigated monitoring
     */
    private void assignToExistingInstanceGroup(GcpContext context,
            Group group, Instance instance, List<CloudResource> buildableResource) throws IOException {
        Compute compute = context.getCompute();
        String projectId = context.getProjectId();
        String zone = context.getLocation().getAvailabilityZone().value();

        List<CloudResource> instanceGroupResources = filterGroupFromName(filterResourcesByType(context.getGroupResources(group.getName()),
                ResourceType.GCP_INSTANCE_GROUP), group.getName());

        if (!instanceGroupResources.isEmpty() && doesGcpInstanceGroupExist(compute, projectId, zone, instanceGroupResources.get(0))) {
            LOGGER.info("adding instance {} to group {} in project {}", instance.getName(), group.getName(), projectId);
            InstanceGroupsAddInstancesRequest request = createAddInstancesRequest(instance, projectId, zone);
            AddInstances addInstances = compute.instanceGroups().addInstances(projectId,
                    zone, instanceGroupResources.get(0).getName(), request);
            try {
                Operation execute = addInstances.execute();
                verifyOperation(execute, buildableResource);
            } catch (GoogleJsonResponseException e) {
                LOGGER.error("Error in Google response, unable to add instance {} to group {} : {} for {}",
                        instance.getName(), group.getName(), e.getMessage(), e.getDetails().getMessage());
                throw new GcpResourceException(checkException(e), resourceType(), buildableResource.get(0).getName());
            }
        } else {
            LOGGER.info("skipping group assignment {} doesn't exist in project {}", group.getName(), projectId);
        }

    }

    private InstanceGroupsAddInstancesRequest createAddInstancesRequest(Instance instance, String projectId, String zone) {
        return new InstanceGroupsAddInstancesRequest().setInstances(List.of(new InstanceReference()
                .setInstance(String.format(INSTANCE_REFERENCE_URI,
                        projectId, zone, instance.getName()))));
    }

    private boolean doesGcpInstanceGroupExist(Compute compute, String projectId, String zone, CloudResource instanceGroupResource) throws IOException {
        InstanceGroupList instanceGroupList = compute.instanceGroups().list(projectId, zone).execute();
        return instanceGroupList.getItems().stream().anyMatch(item -> item.getName().equals(instanceGroupResource.getName()));
    }

    private void addTag(List<String> tagList, String actualTag) {
        if (!tagList.contains(actualTag)) {
            tagList.add(actualTag);
        }
    }

    @VisibleForTesting
    String getPublicKey(String groupPublicKey, String groupLoginUserName) {
        String publicKey;
        String[] publicKeySegments = groupPublicKey.split(" ");
        if (publicKeySegments.length >= 2) {
            publicKey = new StringBuilder()
                    .append(groupLoginUserName)
                    .append(":")
                    .append(publicKeySegments[0])
                    .append(" ")
                    .append(publicKeySegments[1])
                    .append(" ")
                    .append(groupLoginUserName)
                    .toString();
        } else {
            publicKey = groupPublicKey;
        }
        return publicKey;
    }

    private String getHostname(Group group, Long privateId) {
        String hostname = null;
        CloudInstance cloudInstance = group.getInstances().stream()
                .filter(i -> i.getTemplate().getPrivateId().equals(privateId))
                .findFirst()
                .orElse(null);
        if (cloudInstance != null) {
            hostname = cloudInstance.getStringParameter(CloudInstance.DISCOVERY_NAME);
            LOGGER.debug("Setting FreeIPA hostname to {}", hostname);
        }
        return hostname;
    }

    private static String mergeAndTrimKV(String key, String value, char middle, int maxLen) {
        return StringUtils.left(key + middle + value, maxLen);
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
            resource.setStatus(CommonStatus.CREATED);
            persistenceNotifier.notifyUpdate(resource, auth.getCloudContext());
        }
    }

    private List<ServiceAccount> extractServiceAccounts(Group group) {
        String email = group.getIdentity().map(cloudFileSystemView -> {
            CloudGcsView cloudGcsView = CloudGcsView.class.cast(cloudFileSystemView);
            return cloudGcsView.getServiceAccountEmail();
        }).orElse(null);
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
        String location = Strings.isNullOrEmpty(resource.getAvailabilityZone()) ?
                context.getLocation().getAvailabilityZone().value() : resource.getAvailabilityZone();
        try {
            LOGGER.info("Creating operation to delete disk [name: {}] in project [id: {}] in the following availability zone: {}", resourceName,
                    context.getProjectId(), location);
            Operation operation = context.getCompute().instances()
                    .delete(context.getProjectId(), location, resourceName).execute();
            return createOperationAwareCloudResource(resource, operation);
        } catch (GoogleJsonResponseException e) {
            exceptionHandler(e, resourceName, resourceType());
        }
        return null;
    }

    @Override
    public List<CloudVmInstanceStatus> checkInstances(GcpContext context, AuthenticatedContext auth, List<CloudInstance> instances) {
        List<CloudVmInstanceStatus> result = new ArrayList<>();
        String instanceName = instances.isEmpty() ? "" : instances.get(0).getInstanceId();
        if (!StringUtils.isEmpty(instanceName)) {
            List<Instance> gcpInstances = gcpNetworkInterfaceProvider.getInstances(auth, instanceName.split("-")[0]);
            for (CloudInstance instance : instances) {
                Optional<Instance> gcpInstanceOpt = gcpInstances.stream().filter(inst -> inst.getName().equalsIgnoreCase(instance.getInstanceId())).findFirst();
                if (gcpInstanceOpt.isPresent()) {
                    Instance gcpInstance = gcpInstanceOpt.get();
                    InstanceStatus status;
                    switch (gcpInstance.getStatus()) {
                        case "RUNNING":
                            status = InstanceStatus.STARTED;
                            break;
                        case "TERMINATED":
                            status = InstanceStatus.STOPPED;
                            break;
                        default:
                            status = InstanceStatus.IN_PROGRESS;
                    }
                    result.add(new CloudVmInstanceStatus(instance, status));
                } else {
                    LOGGER.warn("Instance {} cannot be found", instance.getInstanceId());
                    result.add(new CloudVmInstanceStatus(instance, InstanceStatus.TERMINATED));
                }
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

    private Collection<AttachedDisk> getBootDiskList(List<CloudResource> resources, String projectId, String zone) {
        Collection<AttachedDisk> listOfDisks = new ArrayList<>();
        for (CloudResource resource : filterResourcesByType(resources, ResourceType.GCP_DISK)) {
            listOfDisks.add(createDisk(projectId, true, resource.getName(), zone, true));
        }
        return listOfDisks;
    }

    private Collection<AttachedDisk> getAttachedDisks(List<CloudResource> resources, String projectId) {
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

    private List<NetworkInterface> getNetworkInterface(GcpContext context, List<CloudResource> computeResources, Group group,
            CloudStack stack, CloudInstance instance) throws IOException {

        boolean noPublicIp = context.getNoPublicIp();
        String projectId = context.getProjectId();
        Location location = context.getLocation();
        Compute compute = context.getCompute();

        NetworkInterface networkInterface = new NetworkInterface();
        String networkName = Strings.isNullOrEmpty(instance.getSubnetId()) ? filterResourcesByType(context.getNetworkResources(),
                ResourceType.GCP_NETWORK).get(0).getName() : instance.getSubnetId();
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
        prepareNetworkAndSubnet(projectId, location.getRegion(), stack.getNetwork(), networkInterface, instance);
        return singletonList(networkInterface);
    }

    private void prepareNetworkAndSubnet(String projectId, Region region, Network network, NetworkInterface networkInterface,
            CloudInstance instance) {
        String subnetId = Strings.isNullOrEmpty(instance.getSubnetId()) ? gcpStackUtil.getSubnetId(network) : instance.getSubnetId();
        if (StringUtils.isNotEmpty(gcpStackUtil.getSharedProjectId(network))) {
            networkInterface.setNetwork(gcpStackUtil.getNetworkUrl(gcpStackUtil.getSharedProjectId(network), gcpStackUtil.getCustomNetworkId(network)));
            networkInterface.setSubnetwork(gcpStackUtil.getSubnetUrl(gcpStackUtil.getSharedProjectId(network), region.value(), subnetId));
        } else {
            networkInterface.setSubnetwork(gcpStackUtil.getSubnetUrl(projectId, region.value(), subnetId));
        }
    }

    private List<CloudResource> filterResourcesByType(List<CloudResource> resources, ResourceType resourceType) {
        return Optional.ofNullable(resources).orElseGet(List::of).stream()
                .filter(resource -> resourceType.equals(resource.getType()))
                .collect(Collectors.toList());
    }

    private List<CloudResource> filterGroupFromName(List<CloudResource> resources, String filterString) {
        return Optional.ofNullable(resources).orElseGet(List::of).stream()
                .filter(resource -> resource.getName().endsWith(filterString))
                .collect(Collectors.toList());
    }

    private CloudVmInstanceStatus stopStart(GcpContext context, AuthenticatedContext auth, CloudInstance instance, boolean stopRequest) {
        String projectId = gcpStackUtil.getProjectId(auth.getCloudCredential());
        String availabilityZone = instance.getAvailabilityZone();
        Compute compute = context.getCompute();
        String instanceId = instance.getInstanceId();
        try {
            LOGGER.info("Gcp operations are preparing: instanceId: {}, projectId: {}, availabilityZone: {}", instanceId, projectId, availabilityZone);
            Get get = compute.instances().get(projectId, availabilityZone, instanceId);
            Instance instanceResponse = get.execute();
            Operation operation;
            if (stopRequest) {
                LOGGER.info("Stop operation executed");
                operation = compute.instances().stop(projectId, availabilityZone, instanceId).setPrettyPrint(true).execute();
            } else {
                LOGGER.info("Start operation executed");
                operation = executeStartOperation(projectId, availabilityZone, compute, instanceId, instance.getTemplate(), instanceResponse.getDisks());
            }
            CloudInstance operationAwareCloudInstance = createOperationAwareCloudInstance(instance, operation);
            return new CloudVmInstanceStatus(operationAwareCloudInstance, InstanceStatus.IN_PROGRESS);
        } catch (TokenResponseException e) {
            throw gcpStackUtil.getMissingServiceAccountKeyError(e, context.getProjectId());
        } catch (IOException e) {
            throw new GcpResourceException(String.format("An error occurred while stopping the vm '%s'", instanceId), e);
        }
    }

    private Operation executeStartOperation(String projectId, String availabilityZone, Compute compute, String instanceId, InstanceTemplate template,
            List<AttachedDisk> disks) throws IOException {

        if (customGcpDiskEncryptionService.hasCustomEncryptionRequested(template)) {
            CustomerEncryptionKey customerEncryptionKey = customGcpDiskEncryptionCreatorService.createCustomerEncryptionKey(template);
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
