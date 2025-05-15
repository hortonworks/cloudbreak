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
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

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
import com.google.api.services.compute.model.AttachedDiskInitializeParams;
import com.google.api.services.compute.model.CustomerEncryptionKey;
import com.google.api.services.compute.model.CustomerEncryptionKeyProtectedDisk;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceGroup;
import com.google.api.services.compute.model.InstanceGroupsAddInstancesRequest;
import com.google.api.services.compute.model.InstanceReference;
import com.google.api.services.compute.model.InstancesStartWithEncryptionKeyRequest;
import com.google.api.services.compute.model.Metadata;
import com.google.api.services.compute.model.Metadata.Items;
import com.google.api.services.compute.model.NetworkInterface;
import com.google.api.services.compute.model.Operation;
import com.google.api.services.compute.model.Scheduling;
import com.google.api.services.compute.model.ServiceAccount;
import com.google.api.services.compute.model.ShieldedInstanceConfig;
import com.google.api.services.compute.model.Tags;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.UpdateType;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpPlatformParameters.GcpDiskType;
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

    private static final String GCP_DISK_TYPE = "PERSISTENT";

    private static final String GCP_SCRATCH_DISK = "SCRATCH";

    private static final String GCP_DISK_MODE = "READ_WRITE";

    private static final String PREEMPTIBLE = "preemptible";

    private static final String GCP_CLOUD_STORAGE_RW_SCOPE = "https://www.googleapis.com/auth/devstorage.read_write";

    private static final int ORDER = 3;

    private static final String INSTANCE_REFERENCE_URI = "https://www.googleapis.com/compute/v1/projects/%s/zones/%s/instances/%s";

    private static final String MACHINETYPE_URL = "https://www.googleapis.com/compute/v1/projects/%s/zones/%s/machineTypes/%s";

    private static final String ZONES_DISK_TYPES_LOCAL_SSD = "zones/%s/diskTypes/local-ssd";

    private static final String DISK_URL = "https://www.googleapis.com/compute/v1/projects/%s/zones/%s/disks/%s";

    @Inject
    private CustomGcpDiskEncryptionCreatorService customGcpDiskEncryptionCreatorService;

    @Inject
    private CustomGcpDiskEncryptionService customGcpDiskEncryptionService;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Inject
    private GcpStackUtil gcpStackUtil;

    @Inject
    private GcpLabelUtil gcpLabelUtil;

    @Inject
    private GcpInstanceStateChecker instanceStateChecker;

    @Inject
    private EntitlementService entitlementService;

    @Override
    public List<CloudResource> create(GcpContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group, Image image) {
        CloudContext cloudContext = auth.getCloudContext();
        String resourceName = getResourceNameService().instance(cloudContext.getName(), group.getName(), privateId);
        CloudResource cloudResource = CloudResource.builder()
                .cloudResource(createNamedResource(resourceType(), resourceName, instance.getAvailabilityZone()))
                .withPrivateId(privateId)
                .build();
        return singletonList(cloudResource);
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
        listOfDisks.addAll(getLocalSsds(computeResources, projectId));

        listOfDisks.forEach(disk -> customGcpDiskEncryptionService.addEncryptionKeyToDisk(template, disk));

        Instance instance = new Instance();
        instance.setMachineType(String.format(MACHINETYPE_URL, projectId, location, template.getFlavor()));
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
        instance.setCanIpForward(Boolean.FALSE);
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

        configureTagsOnInstance(auth, group, instance);
        configureLabelsOnInstance(cloudStack, instance);

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
        startupScript.setValue(cloudStack.getUserDataByType(group.getType()));

        metadata.getItems().add(sshMetaData);
        metadata.getItems().add(startupScript);
        metadata.getItems().add(blockProjectWideSsh);
        instance.setMetadata(metadata);

        ShieldedInstanceConfig shieldedInstanceConfig = Objects.requireNonNullElse(instance.getShieldedInstanceConfig(), new ShieldedInstanceConfig());
        shieldedInstanceConfig.setEnableSecureBoot(entitlementService.isGcpSecureBootEnabled(ThreadBasedUserCrnProvider.getAccountId()));
        instance.setShieldedInstanceConfig(shieldedInstanceConfig);

        Insert insert = compute.instances().insert(projectId, cloudInstance.getAvailabilityZone(), instance);
        insert.setPrettyPrint(Boolean.TRUE);
        try {
            Operation operation = insert.execute();
            verifyOperation(operation, buildableResource);
            updateDiskSetWithInstanceName(auth, computeResources, instance);
            assignToExistingInstanceGroup(context, group, instance, buildableResource, cloudInstance.getAvailabilityZone());
            return singletonList(createOperationAwareCloudResource(buildableResource.get(0), operation));
        } catch (GoogleJsonResponseException e) {
            throw new GcpResourceException(checkException(e), resourceType(), buildableResource.get(0).getName());
        }
    }

    private void configureLabelsOnInstance(CloudStack cloudStack, Instance instance) {
        Map<String, String> labelsFromTags = gcpLabelUtil.createLabelsFromTags(cloudStack);
        instance.setLabels(labelsFromTags);
    }

    private void configureTagsOnInstance(AuthenticatedContext auth, Group group, Instance instance) {
        List<String> tagList = new ArrayList<>();
        addTagIfNotEmpty(tagList, gcpStackUtil.convertGroupName(group.getName()));
        addTagIfNotEmpty(tagList, gcpStackUtil.getNetworkSecurityIdFromGroup(group));
        addTagIfNotEmpty(tagList, gcpStackUtil.getClusterTag(auth.getCloudContext()));
        addTagIfNotEmpty(tagList, gcpStackUtil.getGroupClusterTag(auth.getCloudContext(), group));
        addTagIfNotEmpty(tagList, gcpStackUtil.getGroupTypeTag(group.getType()));
        Tags tags = new Tags();
        tags.setItems(tagList);
        instance.setTags(tags);
    }

    /**
     * if a InstanceGroup was created in GCP for this Instance's group, then after creating this compute instance assign it to that group.
     * the group in general can be used to manage all instances in the same group, specifiaclly one way is used to assign to a load balancer.
     * also provides aggrigated monitoring
     */
    private void assignToExistingInstanceGroup(GcpContext context,
        Group group, Instance instance, List<CloudResource> buildableResource, String zone) throws IOException {
        Compute compute = context.getCompute();
        String projectId = context.getProjectId();

        List<CloudResource> instanceGroupResources = filterGroupFromNameAndAz(filterResourcesByTypes(context.getGroupResources(group.getName()),
                List.of(ResourceType.GCP_INSTANCE_GROUP)), group.getName(), zone);

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
            LOGGER.warn("skipping group assignment {} doesn't exist in project {}", group.getName(), projectId);
        }

    }

    private InstanceGroupsAddInstancesRequest createAddInstancesRequest(Instance instance, String projectId, String zone) {
        return new InstanceGroupsAddInstancesRequest().setInstances(List.of(new InstanceReference()
                .setInstance(String.format(INSTANCE_REFERENCE_URI,
                        projectId, zone, instance.getName()))));
    }

    private boolean doesGcpInstanceGroupExist(Compute compute, String projectId, String zone, CloudResource instanceGroupResource) throws IOException {
        InstanceGroup instanceGroup = compute.instanceGroups().get(projectId, zone, instanceGroupResource.getName()).execute();
        return !instanceGroup.isEmpty();
    }

    private void addTagIfNotEmpty(List<String> tagList, String actualTag) {
        if (!tagList.contains(actualTag) && StringUtils.isNotEmpty(actualTag)) {
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
        for (CloudResource resource : filterResourcesByTypes(computeResources, List.of(ResourceType.GCP_ATTACHED_DISKSET, ResourceType.GCP_DISK))) {
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
        return instanceStateChecker.checkBasedOnOperation(context, instances);
    }

    @Override
    public CloudVmInstanceStatus stop(GcpContext context, AuthenticatedContext auth, CloudInstance instance) {
        return stopStart(context, auth, instance, true);
    }

    @Override
    public CloudResource update(GcpContext context, CloudResource resource, CloudInstance cloudInstance,
            AuthenticatedContext auth, CloudStack cloudStack, Optional<String> targetGroupName, UpdateType updateType) throws Exception {
        String projectId = gcpStackUtil.getProjectId(auth.getCloudCredential());
        String availabilityZone = cloudInstance.getAvailabilityZone();
        Compute compute = context.getCompute();
        String instanceId = cloudInstance.getInstanceId();
        try {
            if (!targetGroupName.isEmpty() && cloudInstance.getTemplate().getGroupName().equalsIgnoreCase(targetGroupName.get())) {
                LOGGER.info("Gcp operations are preparing: group: {}, instanceId: {}, projectId: {}, availabilityZone: {}",
                        targetGroupName.get(), instanceId, projectId, availabilityZone);
                Get get = compute.instances().get(projectId, availabilityZone, instanceId);
                Instance gcpInstance = get.execute();
                gcpInstance.setMachineType(String.format(MACHINETYPE_URL,
                        projectId, availabilityZone, cloudInstance.getTemplate().getFlavor()));
                Compute.Instances.Update update = compute.instances().update(projectId, availabilityZone, gcpInstance.getName(), gcpInstance);
                Operation operation = update.execute();
                LOGGER.debug("Operation with {} successfully inited on {} instance.", operation.getName(), cloudInstance.getInstanceId());
                return createOperationAwareCloudResource(resource, operation);
            } else {
                return null;
            }
        } catch (TokenResponseException e) {
            throw gcpStackUtil.getMissingServiceAccountKeyError(e, context.getProjectId());
        } catch (IOException e) {
            throw new GcpResourceException(String.format("An error occurred while updating the vm '%s'", instanceId), e);
        }
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

    @Override
    public boolean isInstanceBuilder() {
        return true;
    }

    private Collection<AttachedDisk> getBootDiskList(List<CloudResource> resources, String projectId, String zone) {
        Collection<AttachedDisk> listOfDisks = new ArrayList<>();
        for (CloudResource resource : filterResourcesByTypes(resources, List.of(ResourceType.GCP_DISK))) {
            listOfDisks.add(createDisk(
                    projectId,
                    true,
                    resource.getName(),
                    zone,
                    GCP_DISK_TYPE,
                    true,
                    Optional.empty()));
        }
        return listOfDisks;
    }

    private Collection<AttachedDisk> getAttachedDisks(List<CloudResource> resources, String projectId) {
        Collection<AttachedDisk> listOfDisks = new ArrayList<>();
        for (CloudResource resource : filterResourcesByTypes(resources, List.of(ResourceType.GCP_ATTACHED_DISKSET))) {
            VolumeSetAttributes volumeSetAttributes = resource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
            for (Volume volume : volumeSetAttributes.getVolumes()) {
                if (!GcpDiskType.LOCAL_SSD.value().equals(volume.getType())) {
                    listOfDisks.add(createDisk(
                            projectId,
                            false,
                            volume.getId(),
                            volumeSetAttributes.getAvailabilityZone(),
                            GCP_DISK_TYPE,
                            false,
                            Optional.of(volume)));
                }
            }
        }
        return listOfDisks;
    }

    private Collection<AttachedDisk> getLocalSsds(List<CloudResource> resources, String projectId) {
        Collection<AttachedDisk> listOfDisks = new ArrayList<>();
        for (CloudResource resource : filterResourcesByTypes(resources, List.of(ResourceType.GCP_ATTACHED_DISKSET))) {
            VolumeSetAttributes volumeSetAttributes = resource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
            for (Volume volume : volumeSetAttributes.getVolumes()) {
                if (GcpDiskType.LOCAL_SSD.value().equals(volume.getType())) {
                    listOfDisks.add(createDisk(
                            projectId,
                            false,
                            volume.getId(),
                            volumeSetAttributes.getAvailabilityZone(),
                            GCP_SCRATCH_DISK,
                            true,
                            Optional.of(volume)));
                }
            }
        }
        return listOfDisks;
    }

    private AttachedDisk createDisk(String projectId, boolean boot, String resourceName,
        String zone, String diskType, boolean autoDelete, Optional<Volume> volume) {
        AttachedDisk attachedDisk = new AttachedDisk();
        attachedDisk.setBoot(boot);
        attachedDisk.setAutoDelete(autoDelete);
        attachedDisk.setType(diskType);
        attachedDisk.setMode(GCP_DISK_MODE);
        attachedDisk.setDeviceName(resourceName);
        if (!GCP_SCRATCH_DISK.equals(diskType)) {
            attachedDisk.setSource(String.format(DISK_URL, projectId, zone, resourceName));
        } else {
            attachedDisk.setInterface("NVME");
            AttachedDiskInitializeParams attachedDiskInitializeParams =
                    new AttachedDiskInitializeParams()
                            .setDiskSizeGb((long) volume.get().getSize())
                            .setDiskType(String.format(ZONES_DISK_TYPES_LOCAL_SSD, zone));
            attachedDisk.setInitializeParams(attachedDiskInitializeParams);
        }
        return attachedDisk;
    }

    private List<NetworkInterface> getNetworkInterface(GcpContext context, List<CloudResource> computeResources, Group group,
        CloudStack stack, CloudInstance instance) throws IOException {

        boolean noPublicIp = context.getNoPublicIp();
        String projectId = context.getProjectId();
        Location location = context.getLocation();
        Compute compute = context.getCompute();

        NetworkInterface networkInterface = new NetworkInterface();
        String networkName = Strings.isNullOrEmpty(instance.getSubnetId()) ? filterResourcesByTypes(context.getNetworkResources(),
                List.of(ResourceType.GCP_NETWORK)).get(0).getName() : instance.getSubnetId();
        networkInterface.setName(networkName);

        if (!noPublicIp) {
            AccessConfig accessConfig = new AccessConfig();
            accessConfig.setName(networkName);
            accessConfig.setType("ONE_TO_ONE_NAT");
            List<CloudResource> reservedIp = filterResourcesByTypes(computeResources, List.of(ResourceType.GCP_RESERVED_IP));
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

    private List<CloudResource> filterResourcesByTypes(List<CloudResource> resources, List<ResourceType> resourceTypes) {
        return Optional.ofNullable(resources).orElseGet(List::of).stream()
                .filter(resource -> resourceTypes.contains(resource.getType()))
                .collect(Collectors.toList());
    }

    private List<CloudResource> filterGroupFromNameAndAz(List<CloudResource> resources, String filterString, String zone) {
        return Optional.ofNullable(resources).orElseGet(List::of).stream()
                .filter(resource -> resource.getGroup().equals(filterString) && resource.getAvailabilityZone().equals(zone))
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
                operation = compute.instances().stop(projectId, availabilityZone, instanceId)
                        .setPrettyPrint(true)
                        .setDiscardLocalSsd(true)
                        .execute();
            } else {
                LOGGER.info("Start operation executed");
                operation = executeStartOperation(projectId, availabilityZone, compute, instanceId, instance.getTemplate(), instanceResponse.getDisks());
            }
            CloudInstance operationAwareCloudInstance = createOperationAwareCloudInstance(instance, operation);
            LOGGER.debug("Operation with {} successfully inited on {} instance.", operation.getName(), instance.getInstanceId());
            return new CloudVmInstanceStatus(operationAwareCloudInstance, InstanceStatus.IN_PROGRESS);
        } catch (TokenResponseException e) {
            throw gcpStackUtil.getMissingServiceAccountKeyError(e, context.getProjectId());
        } catch (IOException e) {
            String message = String.format("An error occurred while %s the vm '%s'", stopRequest ? "stopping" : "starting", instanceId);
            LOGGER.warn(message, e);
            throw new GcpResourceException(message, e);
        }
    }

    private Operation executeStartOperation(String projectId, String availabilityZone, Compute compute, String instanceId, InstanceTemplate template,
            List<AttachedDisk> disks) throws IOException {

        if (customGcpDiskEncryptionService.hasCustomEncryptionRequested(template)) {
            LOGGER.info("Start the instance with custom encryption: {}", instanceId);
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
            LOGGER.info("Start the instance with instanceId: {}", instanceId);
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
