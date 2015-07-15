package com.sequenceiq.cloudbreak.service.stack.resource.gcp.builders.instance;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_GCP_AND_AZURE_USER_NAME;
import static com.sequenceiq.cloudbreak.domain.InstanceGroupType.isGateway;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.core.annotation.Order;
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
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.domain.CloudRegion;
import com.sequenceiq.cloudbreak.domain.GcpCredential;
import com.sequenceiq.cloudbreak.domain.GcpDiskMode;
import com.sequenceiq.cloudbreak.domain.GcpDiskType;
import com.sequenceiq.cloudbreak.domain.GcpTemplate;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.gcp.GcpRemoveCheckerStatus;
import com.sequenceiq.cloudbreak.service.stack.connector.gcp.GcpRemoveReadyPollerObject;
import com.sequenceiq.cloudbreak.service.stack.connector.gcp.GcpResourceCheckerStatus;
import com.sequenceiq.cloudbreak.service.stack.connector.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.service.stack.connector.gcp.GcpResourceReadyPollerObject;
import com.sequenceiq.cloudbreak.service.stack.connector.gcp.GcpStackUtil;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceNameService;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.GcpSimpleInstanceResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.model.GcpDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.model.GcpProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.model.GcpStartStopContextObject;

@Component
@Order(3)
public class GcpInstanceResourceBuilder extends GcpSimpleInstanceResourceBuilder {

    private static final int END_INDEX = 62;
    @Inject
    private StackRepository stackRepository;
    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;
    @Inject
    private ClusterRepository clusterRepository;
    @Inject
    private GcpResourceCheckerStatus gcpResourceCheckerStatus;
    @Inject
    private PollingService<GcpResourceReadyPollerObject> gcpInstanceReadyPollerObjectPollingService;
    @Inject
    private GcpRemoveCheckerStatus gcpRemoveCheckerStatus;
    @Inject
    private PollingService<GcpRemoveReadyPollerObject> gcpRemoveReadyPollerObjectPollingService;
    @Inject
    private GcpStackUtil gcpStackUtil;
    @Inject
    @Named("GcpResourceNameService")
    private ResourceNameService resourceNameService;

    @Override
    public Boolean create(final CreateResourceRequest createResourceRequest, String region) throws Exception {
        final GcpInstanceCreateRequest gICR = (GcpInstanceCreateRequest) createResourceRequest;
        Stack stack = stackRepository.findById(gICR.getStackId());
        Compute.Instances.Insert ins =
                gICR.getCompute().instances().insert(gICR.getProjectId(), CloudRegion.valueOf(stack.getRegion()).value(), gICR.getInstance());
        ins.setPrettyPrint(Boolean.TRUE);
        Operation execute = ins.execute();
        if (execute.getHttpErrorStatusCode() == null) {
            Compute.ZoneOperations.Get zoneOperations = createZoneOperations(gICR.getCompute(), gICR.getGcpCredential(), execute,
                    CloudRegion.valueOf(stack.getRegion()));
            GcpResourceReadyPollerObject instReady =
                    new GcpResourceReadyPollerObject(zoneOperations, stack, gICR.getInstance().getName(), execute.getName(), ResourceType.GCP_INSTANCE);
            gcpInstanceReadyPollerObjectPollingService.pollWithTimeout(gcpResourceCheckerStatus, instReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
            return true;
        } else {
            throw new GcpResourceException(execute.getHttpErrorMessage(), resourceType(), gICR.getInstance().getName());
        }
    }

    private List<AttachedDisk> getAttachedDisks(List<Resource> resources, GcpCredential gcpCredential, CloudRegion zone) {
        List<AttachedDisk> listOfDisks = new ArrayList<>();
        for (Resource resource : filterResourcesByType(resources, ResourceType.GCP_ATTACHED_DISK)) {
            AttachedDisk attachedDisk = new AttachedDisk();
            attachedDisk.setBoot(false);
            attachedDisk.setAutoDelete(true);
            attachedDisk.setType(GcpDiskType.PERSISTENT.getValue());
            attachedDisk.setMode(GcpDiskMode.READ_WRITE.getValue());
            attachedDisk.setDeviceName(resource.getResourceName());
            attachedDisk.setSource(String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/disks/%s",
                    gcpCredential.getProjectId(), zone.value(), resource.getResourceName()));
            listOfDisks.add(attachedDisk);
        }
        return listOfDisks;
    }

    private List<AttachedDisk> getBootDiskList(List<Resource> resources, GcpCredential gcpCredential, CloudRegion zone) {
        List<AttachedDisk> listOfDisks = new ArrayList<>();
        for (Resource resource : filterResourcesByType(resources, ResourceType.GCP_DISK)) {
            AttachedDisk attachedDisk = new AttachedDisk();
            attachedDisk.setBoot(true);
            attachedDisk.setAutoDelete(true);
            attachedDisk.setType(GcpDiskType.PERSISTENT.getValue());
            attachedDisk.setMode(GcpDiskMode.READ_WRITE.getValue());
            attachedDisk.setDeviceName(resource.getResourceName());
            attachedDisk.setSource(String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/disks/%s",
                    gcpCredential.getProjectId(), zone.value(), resource.getResourceName()));
            listOfDisks.add(attachedDisk);
        }
        return listOfDisks;
    }

    @Override
    public Boolean delete(Resource resource, GcpDeleteContextObject deleteContextObject, String region) throws Exception {
        Stack stack = stackRepository.findByIdLazy(deleteContextObject.getStackId());
        try {
            GcpCredential gcpCredential = (GcpCredential) stack.getCredential();
            Operation operation = deleteContextObject.getCompute().instances()
                    .delete(gcpCredential.getProjectId(), CloudRegion.valueOf(region).value(), resource.getResourceName()).execute();
            Compute.ZoneOperations.Get zoneOperations = createZoneOperations(deleteContextObject.getCompute(),
                    gcpCredential, operation, CloudRegion.valueOf(region));
            Compute.GlobalOperations.Get globalOperations = createGlobalOperations(deleteContextObject.getCompute(), gcpCredential, operation);
            GcpRemoveReadyPollerObject gcpRemoveReady =
                    new GcpRemoveReadyPollerObject(zoneOperations, globalOperations, stack, resource.getResourceName(), operation.getName(), resourceType());
            gcpRemoveReadyPollerObjectPollingService.pollWithTimeout(gcpRemoveCheckerStatus, gcpRemoveReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        } catch (GoogleJsonResponseException ex) {
            exceptionHandler(ex, resource.getResourceName(), stack);
        } catch (IOException e) {
            throw new GcpResourceException(e);
        }
        return true;
    }

    @Override
    public List<Resource> buildResources(GcpProvisionContextObject provisionContextObject, int index, List<Resource> resources,
            Optional<InstanceGroup> instanceGroup) {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        String resourceName = resourceNameService.resourceName(resourceType(), stack.getName(), instanceGroup.orNull().getGroupName(), index);
        Resource resource = new Resource(resourceType(), resourceName, stack, instanceGroup.orNull().getGroupName());
        return Arrays.asList(resource);
    }

    @Override
    public CreateResourceRequest buildCreateRequest(GcpProvisionContextObject provisionContextObject, List<Resource> resources,
            List<Resource> buildResources, int index, Optional<InstanceGroup> instanceGroup, Optional<String> userData) throws Exception {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        GcpCredential gcpCredential = (GcpCredential) stack.getCredential();
        GcpTemplate gcpTemplate = (GcpTemplate) instanceGroup.orNull().getTemplate();

        List<AttachedDisk> listOfDisks = new ArrayList<>();
        listOfDisks.addAll(getBootDiskList(resources, gcpCredential, CloudRegion.valueOf(stack.getRegion())));
        listOfDisks.addAll(getAttachedDisks(resources, gcpCredential, CloudRegion.valueOf(stack.getRegion())));

        Instance instance = new Instance();
        instance.setMachineType(String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/machineTypes/%s",
                provisionContextObject.getProjectId(), CloudRegion.valueOf(stack.getRegion()).value(), gcpTemplate.getGcpInstanceType().getValue()));
        instance.setName(buildResources.get(0).getResourceName());
        instance.setCanIpForward(Boolean.TRUE);
        instance.setNetworkInterfaces(getNetworkInterface(provisionContextObject, stack.getResources(), CloudRegion.valueOf(stack.getRegion()), instanceGroup));
        instance.setDisks(listOfDisks);
        Tags tags = new Tags();
        tags.setItems(Arrays.asList(instanceGroup.orNull().getGroupName().toLowerCase().toString().replaceAll("[^A-Za-z0-9 ]", "")));
        instance.setTags(tags);
        Metadata metadata = new Metadata();
        metadata.setItems(Lists.<Metadata.Items>newArrayList());

        Metadata.Items sshMetaData = new Metadata.Items();
        sshMetaData.setKey("sshKeys");
        sshMetaData.setValue(CB_GCP_AND_AZURE_USER_NAME + ":" + gcpCredential.getPublicKey());

        Metadata.Items startupScript = new Metadata.Items();
        startupScript.setKey("startup-script");
        startupScript.setValue(userData.orNull());

        metadata.getItems().add(sshMetaData);
        metadata.getItems().add(startupScript);
        instance.setMetadata(metadata);
        return new GcpInstanceCreateRequest(provisionContextObject.getStackId(), resources, instance, provisionContextObject.getProjectId(),
                provisionContextObject.getCompute(), gcpTemplate, gcpCredential, buildResources);
    }

    public Instance describe(Stack stack, Compute compute, Resource resource, CloudRegion region) throws IOException {
        GcpCredential gcpCredential = (GcpCredential) stack.getCredential();
        Compute.Instances.Get getVm = compute.instances().get(gcpCredential.getProjectId(), region.value(),
                resource.getResourceName());
        return getVm.execute();
    }

    private List<NetworkInterface> getNetworkInterface(GcpProvisionContextObject contextObject, Set<Resource> resources, CloudRegion gcpZone,
            Optional<InstanceGroup> instanceGroup) throws IOException {
        NetworkInterface iface = new NetworkInterface();
        String networkName = filterResourcesByType(resources, ResourceType.GCP_NETWORK).get(0).getResourceName();
        iface.setName(networkName);
        AccessConfig accessConfig = new AccessConfig();
        accessConfig.setName(networkName);
        accessConfig.setType("ONE_TO_ONE_NAT");
        if (instanceGroup.isPresent() && isGateway(instanceGroup.orNull().getInstanceGroupType())) {
            Compute.Addresses.Get getReservedIp = contextObject.getCompute().addresses().get(contextObject.getProjectId(), gcpZone.region(),
                    filterResourcesByType(resources, ResourceType.GCP_RESERVED_IP).get(0).getResourceName());
            accessConfig.setNatIP(getReservedIp.execute().getAddress());
        }
        iface.setAccessConfigs(ImmutableList.of(accessConfig));
        iface.setNetwork(String.format("https://www.googleapis.com/compute/v1/projects/%s/global/networks/%s", contextObject.getProjectId(), networkName));
        return Arrays.asList(iface);
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCP_INSTANCE;
    }

    @Override
    public void stop(GcpStartStopContextObject startStopContextObject, Resource resource, String region) {
        GcpCredential credential = (GcpCredential) startStopContextObject.getStack().getCredential();
        try {
            Compute.Instances.Get get = startStopContextObject.getCompute().instances()
                    .get(credential.getProjectId(), CloudRegion.valueOf(region).value(), resource.getResourceName());
            if ("RUNNING".equals(get.execute().getStatus())) {
                Compute.Instances.Stop stop = startStopContextObject.getCompute().instances()
                        .stop(credential.getProjectId(), CloudRegion.valueOf(region).value(), resource.getResourceName());
                stop.setPrettyPrint(Boolean.TRUE);
                setInstanceState(stop.execute(), startStopContextObject, resource, credential, false);
            } else {
                LOGGER.info("Instance is not in RUNNING state - won't stop it.");
            }
        } catch (IOException e) {
            throw new GcpResourceException(String.format("An error occurred while stopping the vm '%s'", resource.getResourceName()), e);
        }
    }

    @Override
    public void start(GcpStartStopContextObject startStopContextObject, Resource resource, String region) {
        GcpCredential credential = (GcpCredential) startStopContextObject.getStack().getCredential();
        try {
            Compute.Instances.Get get = startStopContextObject.getCompute().instances()
                    .get(credential.getProjectId(), CloudRegion.valueOf(region).value(), resource.getResourceName());
            if ("TERMINATED".equals(get.execute().getStatus())) {
                Compute.Instances.Start start = startStopContextObject.getCompute().instances()
                        .start(credential.getProjectId(), CloudRegion.valueOf(region).value(), resource.getResourceName());
                start.setPrettyPrint(Boolean.TRUE);
                setInstanceState(start.execute(), startStopContextObject, resource, credential, true);
            } else {
                LOGGER.info("Instance is not in TERMINATED state - won't start it.");
            }
        } catch (IOException e) {
            throw new GcpResourceException(String.format("An error occurred while starting the vm '%s'", resource.getResourceName()), e);
        }
    }

    private void setInstanceState(Operation operation, GcpStartStopContextObject startStopContextObject, Resource resource,
            GcpCredential credential, boolean start) throws IOException {
        if (operation.getHttpErrorStatusCode() == null) {
            Compute.ZoneOperations.Get zoneOperations = createZoneOperations(
                    startStopContextObject.getCompute(),
                    credential,
                    operation,
                    CloudRegion.valueOf(startStopContextObject.getStack().getRegion()));
            GcpResourceReadyPollerObject instReady = new GcpResourceReadyPollerObject(zoneOperations, startStopContextObject.getStack(),
                    resource.getResourceName(), operation.getName(), ResourceType.GCP_INSTANCE);
            gcpInstanceReadyPollerObjectPollingService.pollWithTimeout(gcpResourceCheckerStatus, instReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
            if (start) {
                updateInstanceMetadata(startStopContextObject, resource);
            }
        } else {
            throw new GcpResourceException(String.format("Gcp operation returned an error code: %s, message: %s",
                    operation.getHttpErrorStatusCode(), operation.getHttpErrorMessage()));
        }
    }

    private void updateInstanceMetadata(GcpStartStopContextObject startStopContextObject, Resource resource) {
        Instance instance = gcpStackUtil.getInstance(startStopContextObject.getStack(), startStopContextObject.getCompute(), resource);
        if (instance != null) {
            Stack stack = startStopContextObject.getStack();
            InstanceMetaData metaData = instanceMetaDataRepository.findByInstanceId(stack.getId(), resource.getResourceName());
            String publicIP = instance.getNetworkInterfaces().get(0).getAccessConfigs().get(0).getNatIP();
            metaData.setPublicIp(publicIP);
            instanceMetaDataRepository.save(metaData);
        } else {
            LOGGER.error(String.format("Can't find instance by resource name (instance id) : %s", resource.getResourceName()));
        }
    }

    public class GcpInstanceCreateRequest extends CreateResourceRequest {

        private Long stackId;
        private List<Resource> resources = new ArrayList<>();
        private Instance instance;
        private String projectId;
        private Compute compute;
        private GcpTemplate gcpTemplate;
        private GcpCredential gcpCredential;

        public GcpInstanceCreateRequest(Long stackId, List<Resource> resources, Instance instance,
                String projectId, Compute compute, GcpTemplate gcpTemplate, GcpCredential gcpCredential, List<Resource> buildNames) {
            super(buildNames);
            this.stackId = stackId;
            this.resources = resources;
            this.instance = instance;
            this.projectId = projectId;
            this.compute = compute;
            this.gcpTemplate = gcpTemplate;
            this.gcpCredential = gcpCredential;
        }

        public Long getStackId() {
            return stackId;
        }

        public List<Resource> getResources() {
            return resources;
        }

        public Instance getInstance() {
            return instance;
        }

        public String getProjectId() {
            return projectId;
        }

        public Compute getCompute() {
            return compute;
        }

        public GcpTemplate getGcpTemplate() {
            return gcpTemplate;
        }

        public GcpCredential getGcpCredential() {
            return gcpCredential;
        }
    }

}
