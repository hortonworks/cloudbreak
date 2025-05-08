package com.sequenceiq.cloudbreak.cloud.mock;

import static com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.CREATED;
import static com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.DELETED;
import static com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.FAILED;
import static com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.UPDATED;
import static com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificateType.ROOT;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.UpdateType;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.TemplatingNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificate;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class MockResourceConnector implements ResourceConnector {

    public static final String MOCK_RDS_PORT = "1234";

    public static final String MOCK_RDS_HOST = "mockrdshost";

    private static final Logger LOGGER = LoggerFactory.getLogger(MockResourceConnector.class);

    @Inject
    private PersistenceNotifier resourceNotifier;

    @Inject
    private CloudResourceHelper cloudResourceHelper;

    @Inject
    private MockUrlFactory mockUrlFactory;

    @Override
    public List<CloudResourceStatus> launch(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier persistenceNotifier,
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold) {
        List<CloudVmInstanceStatus> instanceIdIterator = launch(authenticatedContext, stack);
        CloudContext cloudContext = authenticatedContext.getCloudContext();
        return generateResources(cloudContext, instanceIdIterator);
    }

    private List<CloudVmInstanceStatus> launch(AuthenticatedContext authenticatedContext, CloudStack cloudStack) {
        CloudVmInstanceStatus[] cloudVmInstanceStatusArray = mockUrlFactory.get(authenticatedContext, "/spi/launch")
                .post(Entity.entity(cloudStack, MediaType.APPLICATION_JSON_TYPE), CloudVmInstanceStatus[].class);
        return Arrays.asList(cloudVmInstanceStatusArray);
    }

    private List<CloudVmInstanceStatus> resize(AuthenticatedContext authenticatedContext, List<Group> groups) {
        CloudVmInstanceStatus[] cloudVmInstanceStatusArray = mockUrlFactory.get(authenticatedContext, "/spi/add_instance")
                .post(Entity.entity(groups, MediaType.APPLICATION_JSON_TYPE), CloudVmInstanceStatus[].class);
        return Arrays.asList(cloudVmInstanceStatusArray);
    }

    @Override
    public List<CloudResourceStatus> launchLoadBalancers(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier persistenceNotifier)
            throws Exception {
        throw new UnsupportedOperationException("Load balancers are not supported for the mock resource connector.");
    }

    @Override
    public List<CloudResourceStatus> launchDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack,
            PersistenceNotifier persistenceNotifier) {
        //return oldLaunchDbImpl(authenticatedContext, stack, persistenceNotifier);
        return newLaunchDbImpl(authenticatedContext, stack, persistenceNotifier);
    }

    private List<CloudResourceStatus> newLaunchDbImpl(AuthenticatedContext authenticatedContext, DatabaseStack stack, PersistenceNotifier persistenceNotifier) {
        LOGGER.info("launch db on mock spi");
        CloudResourceStatus[] cloudResourceStatuses = mockUrlFactory.get(authenticatedContext, "/db")
                .post(Entity.entity(stack, MediaType.APPLICATION_JSON_TYPE), CloudResourceStatus[].class);
        List<CloudResource> cl = Arrays.stream(cloudResourceStatuses).map(crs -> crs.getCloudResource()).collect(Collectors.toList());
        persistenceNotifier.notifyAllocations(cl, authenticatedContext.getCloudContext());
        return List.of(cloudResourceStatuses);
    }

    private List<CloudResourceStatus> oldLaunchDbImpl(AuthenticatedContext authenticatedContext, DatabaseStack stack, PersistenceNotifier persistenceNotifier) {
        List<CloudResource> cloudResources = List.of(
                CloudResource.builder()
                        .withType(ResourceType.RDS_HOSTNAME)
                        .withStatus(CommonStatus.CREATED)
                        .withName(MOCK_RDS_HOST)
                        .withPersistent(true)
                        .build(),
                CloudResource.builder()
                        .withType(ResourceType.RDS_PORT)
                        .withStatus(CommonStatus.CREATED)
                        .withName(MOCK_RDS_PORT)
                        .withPersistent(true)
                        .build()
        );
        persistenceNotifier.notifyAllocations(cloudResources, authenticatedContext.getCloudContext());
        return cloudResources.stream()
                .map(cr -> new CloudResourceStatus(cr, CREATED))
                .collect(Collectors.toList());
    }

    @Override
    public void validateUpgradeDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack, TargetMajorVersion targetMajorVersion) {
        mockUrlFactory.get(authenticatedContext, "/db/upgrade")
                .post(Entity.entity(targetMajorVersion.getMajorVersion(), MediaType.APPLICATION_JSON_TYPE));
    }

    @Override
    public List<CloudResourceStatus> launchValidateUpgradeDatabaseServerResources(AuthenticatedContext authenticatedContext, DatabaseStack stack,
            TargetMajorVersion targetMajorVersion, DatabaseStack migratedDbStack, PersistenceNotifier persistenceNotifier) {
        return List.of();
    }

    @Override
    public void cleanupValidateUpgradeDatabaseServerResources(AuthenticatedContext authenticatedContext, DatabaseStack stack, List<CloudResource> resources,
            PersistenceNotifier persistenceNotifier) {
    }

    @Override
    public void upgradeDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack originalStack, DatabaseStack stack,
            PersistenceNotifier persistenceNotifier, TargetMajorVersion targetMajorVersion, List<CloudResource> resources) {
        mockUrlFactory.get(authenticatedContext, "/db/upgrade").put(Entity.entity(targetMajorVersion.getMajorVersion(), MediaType.APPLICATION_JSON_TYPE));
    }

    @Override
    public List<CloudResourceStatus> check(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        CloudVmInstanceStatus[] post = mockUrlFactory.get(authenticatedContext, "/spi/cloud_instance_statuses")
                .post(Entity.entity(resources, MediaType.APPLICATION_JSON_TYPE), CloudVmInstanceStatus[].class);
        return generateResources(authenticatedContext.getCloudContext(), Arrays.asList(post));
    }

    @Override
    public List<CloudResourceStatus> terminate(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> cloudResources) {
        try (Response ignore = mockUrlFactory.get(authenticatedContext, "/spi/terminate").delete()) {
        }
        cloudResources.forEach(r -> resourceNotifier.notifyDeletion(r, authenticatedContext.getCloudContext()));
        return cloudResources.stream().map(r -> new CloudResourceStatus(r, DELETED)).collect(Collectors.toList());
    }

    @Override
    public List<CloudResourceStatus> terminateDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack,
            List<CloudResource> resources, PersistenceNotifier persistenceNotifier, boolean force) {
        //return emptyList();
        return newTerminateDatabaseServerImpl(authenticatedContext, stack, resources, persistenceNotifier, force);
    }

    private List<CloudResourceStatus> newTerminateDatabaseServerImpl(AuthenticatedContext authenticatedContext, DatabaseStack stack,
            List<CloudResource> resources, PersistenceNotifier persistenceNotifier, boolean force) {
        mockUrlFactory.get(authenticatedContext, "/db").delete();
        return emptyList();
    }

    @Override
    public List<CloudResourceStatus> update(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources,
            UpdateType type, Optional<String> groupName) {
        List<CloudResourceStatus> cloudResourceStatuses = resources.stream().map(r -> {
                    Group group = stack.getGroups().stream().filter(g -> g.getName().equals(r.getGroup())).findFirst()
                            .orElseThrow(NotFoundException.notFound("Group", r.getGroup()));
                    String path = "/spi/update/" + r.getGroup() + "/instance_type/" + getFlavorFromGroup(group);
                    mockUrlFactory.get(authenticatedContext, path).get(CloudVmInstanceStatus[].class);
                    return new CloudResourceStatus(r, UPDATED);
                })
                .collect(Collectors.toList());
        return cloudResourceStatuses;
    }

    private String getFlavorFromGroup(Group group) {
        if (!CollectionUtils.isEmpty(group.getInstances())) {
            return group.getInstances().getFirst().getTemplate().getFlavor();
        } else if (!CollectionUtils.isEmpty(group.getDeletedInstances())) {
            return group.getDeletedInstances().getFirst().getTemplate().getFlavor();
        } else {
            throw NotFoundException.notFound("Flavor for group.", group.getName()).get();
        }
    }

    @Override
    public void updateUserData(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources,
            Map<InstanceGroupType, String> userData) {

    }

    @Override
    public void checkUpdate(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) throws Exception {
    }

    @Override
    public List<CloudResourceStatus> upscale(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources,
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold) {
        CloudContext cloudContext = authenticatedContext.getCloudContext();
        List<Group> scaledGroups = cloudResourceHelper.getScaledGroups(stack);
        List<Group> groups = scaledGroups.stream()
                .map(g -> {
                    List<CloudInstance> newInstances = g.getInstances().stream()
                            .filter(i -> i.getTemplate().getStatus() == InstanceStatus.CREATE_REQUESTED)
                            .collect(Collectors.toList());
                    return Group.builder()
                            .withName(g.getName())
                            .withType(g.getType())
                            .withInstances(newInstances)
                            .withSecurity(g.getSecurity())
                            .withSkeleton(g.getReferenceInstanceConfiguration())
                            .withInstanceAuthentication(g.getInstanceAuthentication())
                            .withLoginUserName(g.getLoginUserName())
                            .withPublicKey(g.getPublicKey())
                            .withRootVolumeSize(g.getRootVolumeSize())
                            .withIdentity(g.getIdentity())
                            .withNetwork(g.getNetwork())
                            .withTags(g.getTags())
                            .withRootVolumeType(g.getRootVolumeType())
                            .build();
                }).collect(Collectors.toList());

        List<CloudVmInstanceStatus> resized = resize(authenticatedContext, groups);
        return generateResources(cloudContext, resized);
    }

    private List<CloudResourceStatus> generateResources(CloudContext cloudContext, List<CloudVmInstanceStatus> resize) {
        List<CloudResourceStatus> ret = new ArrayList<>();
        List<CloudResource> cloudResources = new ArrayList<>();
        for (CloudVmInstanceStatus cloudVmInstanceStatus : resize) {
            CloudInstance cloudInstance = cloudVmInstanceStatus.getCloudInstance();
            CommonStatus commonStatus = CommonStatus.CREATED;
            if (InstanceStatus.FAILED.equals(cloudVmInstanceStatus.getStatus())) {
                commonStatus = CommonStatus.FAILED;
            }
            ResourceStatus resourceStatus = CREATED;
            if (InstanceStatus.FAILED.equals(cloudVmInstanceStatus.getStatus())) {
                resourceStatus = FAILED;
            }
            CloudResource instanceResource = generateResource("cloudinstance" + cloudInstance.getTemplate().getPrivateId(), cloudInstance,
                    cloudInstance.getInstanceId(), ResourceType.MOCK_INSTANCE, commonStatus);
            cloudResources.add(instanceResource);
            List<Volume> volumes = cloudInstance.getTemplate().getVolumes();
            for (int i = 0; i < volumes.size(); i++) {
                UUID uuid = UUID.randomUUID();
                CloudResource volumeResource = generateResource("cloudvolume" + uuid, cloudInstance, cloudInstance.getInstanceId(),
                        ResourceType.MOCK_VOLUME, commonStatus);
                cloudResources.add(volumeResource);
                ret.add(new CloudResourceStatus(volumeResource, resourceStatus, cloudInstance.getTemplate().getPrivateId()));
            }
            ret.add(new CloudResourceStatus(instanceResource, resourceStatus, cloudInstance.getTemplate().getPrivateId()));
        }
        resourceNotifier.notifyAllocations(cloudResources, cloudContext);
        return ret;
    }

    private CloudResource generateResource(String name, CloudInstance cloudInstance, String instanceId, ResourceType type, CommonStatus commonStatus) {
        CloudResource resource = CloudResource.builder()
                .withType(type)
                .withStatus(commonStatus)
                .withGroup(cloudInstance.getTemplate().getGroupName())
                .withName(name)
                .withInstanceId(instanceId)
                .withParameters(cloudInstance.getParameters())
                .withPersistent(true)
                .build();
        return resource;
    }

    @Override
    public List<CloudResource> collectResourcesToRemove(AuthenticatedContext authenticatedContext, CloudStack stack,
            List<CloudResource> resources, List<CloudInstance> vms) {
        return null;
    }

    @Override
    public List<CloudResourceStatus> downscale(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources,
            List<CloudInstance> vms, List<CloudResource> resourcesToRemove) {
        mockUrlFactory.get(authenticatedContext, "/spi/terminate_instances").post(Entity.entity(vms, MediaType.APPLICATION_JSON_TYPE), String.class);
        List<String> instanceIdsToDownscale = new ArrayList<>();
        for (CloudInstance vm : vms) {
            instanceIdsToDownscale.add(vm.getInstanceId());
        }
        List<CloudResource> resourcesToDownscale = resources.stream()
                .filter(resource -> instanceIdsToDownscale.contains(resource.getInstanceId()))
                .collect(Collectors.toList());
        resourceNotifier.notifyDeletions(resourcesToDownscale, authenticatedContext.getCloudContext());
        return resourcesToDownscale.stream().map(r -> new CloudResourceStatus(r, DELETED)).collect(Collectors.toList());
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
    public String getDBStackTemplate(DatabaseStack databaseStack) {
        return "BestDbStackTemplateInTheWorld";
    }

    @Override
    public void updateDiskVolumes(AuthenticatedContext authenticatedContext, List<String> volumeIds, String diskType, int size) throws Exception {
        throw new UnsupportedOperationException("Method not implemented.");
    }

    @Override
    public void startDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack) {
        mockUrlFactory.get(authenticatedContext, "/db").put(Entity.entity(Boolean.TRUE, MediaType.APPLICATION_JSON_TYPE));
    }

    @Override
    public void stopDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack) {
        mockUrlFactory.get(authenticatedContext, "/db").put(Entity.entity(Boolean.FALSE, MediaType.APPLICATION_JSON_TYPE));
    }

    @Override
    public ExternalDatabaseStatus getDatabaseServerStatus(AuthenticatedContext authenticatedContext, DatabaseStack stack) {
        return mockUrlFactory.get(authenticatedContext, "/db")
                .get(ExternalDatabaseStatus.class);
    }

    @Override
    public CloudDatabaseServerSslCertificate getDatabaseServerActiveSslRootCertificate(AuthenticatedContext authenticatedContext, DatabaseStack stack) {
        String certificate = mockUrlFactory.get(authenticatedContext, "/db/activecertificate")
                .get(String.class);
        return new CloudDatabaseServerSslCertificate(ROOT, certificate);
    }

    @Override
    public void updateDatabaseRootPassword(AuthenticatedContext authenticatedContext, DatabaseStack databaseStack, String newPassword) {
    }

    @Override
    public ResourceType getInstanceResourceType() {
        return ResourceType.MOCK_INSTANCE;
    }
}