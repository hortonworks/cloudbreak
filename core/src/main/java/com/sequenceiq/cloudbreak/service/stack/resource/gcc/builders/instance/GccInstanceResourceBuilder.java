package com.sequenceiq.cloudbreak.service.stack.resource.gcc.builders.instance;

import static com.sequenceiq.cloudbreak.domain.InstanceGroupType.isGateway;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.domain.GccTemplate;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccRemoveCheckerStatus;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccRemoveReadyPollerObject;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccResourceCheckerStatus;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccResourceCreationException;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccResourceReadyPollerObject;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccStackUtil;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccDiskMode;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccDiskType;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccZone;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.GccSimpleInstanceResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccStartStopContextObject;

@Component
@Order(3)
public class GccInstanceResourceBuilder extends GccSimpleInstanceResourceBuilder {

    @Autowired
    private StackRepository stackRepository;
    @Autowired
    private InstanceMetaDataRepository instanceMetaDataRepository;
    @Autowired
    private ClusterRepository clusterRepository;
    @Autowired
    private GccResourceCheckerStatus gccResourceCheckerStatus;
    @Autowired
    private PollingService<GccResourceReadyPollerObject> gccInstanceReadyPollerObjectPollingService;
    @Autowired
    private GccRemoveCheckerStatus gccRemoveCheckerStatus;
    @Autowired
    private PollingService<GccRemoveReadyPollerObject> gccRemoveReadyPollerObjectPollingService;
    @Autowired
    private GccStackUtil gccStackUtil;

    @Override
    public Boolean create(final CreateResourceRequest createResourceRequest, String region) throws Exception {
        final GccInstanceCreateRequest gICR = (GccInstanceCreateRequest) createResourceRequest;
        Stack stack = stackRepository.findById(gICR.getStackId());
        Compute.Instances.Insert ins =
                gICR.getCompute().instances().insert(gICR.getProjectId(), GccZone.valueOf(stack.getRegion()).getValue(), gICR.getInstance());
        ins.setPrettyPrint(Boolean.TRUE);
        Operation execute = ins.execute();
        if (execute.getHttpErrorStatusCode() == null) {
            Compute.ZoneOperations.Get zoneOperations = createZoneOperations(gICR.getCompute(), gICR.getGccCredential(), execute,
                    GccZone.valueOf(stack.getRegion()));
            GccResourceReadyPollerObject instReady =
                    new GccResourceReadyPollerObject(zoneOperations, stack, gICR.getInstance().getName(), execute.getName(), ResourceType.GCC_INSTANCE);
            gccInstanceReadyPollerObjectPollingService.pollWithTimeout(gccResourceCheckerStatus, instReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
            return true;
        } else {
            throw new GccResourceCreationException(execute.getHttpErrorMessage(), resourceType(), gICR.getInstance().getName());
        }
    }

    private List<AttachedDisk> getAttachedDisks(List<Resource> resources, GccCredential gccCredential, GccZone zone) {
        List<AttachedDisk> listOfDisks = new ArrayList<>();
        for (Resource resource : filterResourcesByType(resources, ResourceType.GCC_ATTACHED_DISK)) {
            AttachedDisk attachedDisk = new AttachedDisk();
            attachedDisk.setBoot(false);
            attachedDisk.setAutoDelete(true);
            attachedDisk.setType(GccDiskType.PERSISTENT.getValue());
            attachedDisk.setMode(GccDiskMode.READ_WRITE.getValue());
            attachedDisk.setDeviceName(resource.getResourceName());
            attachedDisk.setSource(String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/disks/%s",
                    gccCredential.getProjectId(), zone.getValue(), resource.getResourceName()));
            listOfDisks.add(attachedDisk);
        }
        return listOfDisks;
    }

    private List<AttachedDisk> getBootDiskList(List<Resource> resources, GccCredential gccCredential, GccZone zone) {
        List<AttachedDisk> listOfDisks = new ArrayList<>();
        for (Resource resource : filterResourcesByType(resources, ResourceType.GCC_DISK)) {
            AttachedDisk attachedDisk = new AttachedDisk();
            attachedDisk.setBoot(true);
            attachedDisk.setAutoDelete(true);
            attachedDisk.setType(GccDiskType.PERSISTENT.getValue());
            attachedDisk.setMode(GccDiskMode.READ_WRITE.getValue());
            attachedDisk.setDeviceName(resource.getResourceName());
            attachedDisk.setSource(String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/disks/%s",
                    gccCredential.getProjectId(), zone.getValue(), resource.getResourceName()));
            listOfDisks.add(attachedDisk);
        }
        return listOfDisks;
    }

    @Override
    public Boolean delete(Resource resource, GccDeleteContextObject deleteContextObject, String region) throws Exception {
        Stack stack = stackRepository.findById(deleteContextObject.getStackId());
        try {
            GccCredential gccCredential = (GccCredential) stack.getCredential();
            Operation operation = deleteContextObject.getCompute().instances()
                    .delete(gccCredential.getProjectId(), GccZone.valueOf(region).getValue(), resource.getResourceName()).execute();
            Compute.ZoneOperations.Get zoneOperations = createZoneOperations(deleteContextObject.getCompute(),
                    gccCredential, operation, GccZone.valueOf(region));
            Compute.GlobalOperations.Get globalOperations = createGlobalOperations(deleteContextObject.getCompute(), gccCredential, operation);
            GccRemoveReadyPollerObject gccRemoveReady =
                    new GccRemoveReadyPollerObject(zoneOperations, globalOperations, stack, resource.getResourceName(), operation.getName(), resourceType());
            gccRemoveReadyPollerObjectPollingService.pollWithTimeout(gccRemoveCheckerStatus, gccRemoveReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        } catch (GoogleJsonResponseException ex) {
            exceptionHandler(ex, resource.getResourceName(), stack);
        } catch (IOException e) {
            throw new InternalServerException(e.getMessage());
        }
        return true;
    }

    @Override
    public List<Resource> buildResources(GccProvisionContextObject provisionContextObject, int index, List<Resource> resources,
            Optional<InstanceGroup> instanceGroup) {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        Resource resource = new Resource(resourceType(),
                String.format("%s-%s-%s", stack.getName(), index, new Date().getTime()), stack,
                instanceGroup.orNull().getGroupName());
        return Arrays.asList(resource);
    }

    @Override
    public CreateResourceRequest buildCreateRequest(GccProvisionContextObject provisionContextObject, List<Resource> resources,
            List<Resource> buildResources, int index, Optional<InstanceGroup> instanceGroup, Optional<String> userData) throws Exception {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        GccCredential gccCredential = (GccCredential) stack.getCredential();
        GccTemplate gccTemplate = (GccTemplate) instanceGroup.orNull().getTemplate();

        List<AttachedDisk> listOfDisks = new ArrayList<>();
        listOfDisks.addAll(getBootDiskList(resources, gccCredential, GccZone.valueOf(stack.getRegion())));
        listOfDisks.addAll(getAttachedDisks(resources, gccCredential, GccZone.valueOf(stack.getRegion())));

        Instance instance = new Instance();
        instance.setMachineType(String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/machineTypes/%s",
                provisionContextObject.getProjectId(), GccZone.valueOf(stack.getRegion()).getValue(), gccTemplate.getGccInstanceType().getValue()));
        instance.setName(buildResources.get(0).getResourceName());
        instance.setCanIpForward(Boolean.TRUE);
        instance.setNetworkInterfaces(getNetworkInterface(provisionContextObject, stack.getResources(), GccZone.valueOf(stack.getRegion()), instanceGroup));
        instance.setDisks(listOfDisks);
        Tags tags = new Tags();
        tags.setItems(Arrays.asList(instanceGroup.orNull().getGroupName().toString().replaceAll("[^A-Za-z0-9 ]", "")));
        instance.setTags(tags);
        Metadata metadata = new Metadata();
        metadata.setItems(Lists.<Metadata.Items>newArrayList());

        Metadata.Items sshMetaData = new Metadata.Items();
        sshMetaData.setKey("sshKeys");
        sshMetaData.setValue("ubuntu:" + gccCredential.getPublicKey());

        Metadata.Items startupScript = new Metadata.Items();
        startupScript.setKey("startup-script");
        startupScript.setValue(userData.orNull());

        metadata.getItems().add(sshMetaData);
        metadata.getItems().add(startupScript);
        instance.setMetadata(metadata);
        return new GccInstanceCreateRequest(provisionContextObject.getStackId(), resources, instance, provisionContextObject.getProjectId(),
                provisionContextObject.getCompute(), gccTemplate, gccCredential, buildResources);
    }

    public Instance describe(Stack stack, Compute compute, Resource resource, GccZone region) throws IOException {
        GccCredential gccCredential = (GccCredential) stack.getCredential();
        Compute.Instances.Get getVm = compute.instances().get(gccCredential.getProjectId(), region.getValue(),
                resource.getResourceName());
        return getVm.execute();
    }

    private List<NetworkInterface> getNetworkInterface(GccProvisionContextObject contextObject, Set<Resource> resources, GccZone gccZone,
            Optional<InstanceGroup> instanceGroup) throws IOException {
        NetworkInterface iface = new NetworkInterface();
        String networkName = filterResourcesByType(resources, ResourceType.GCC_NETWORK).get(0).getResourceName();
        iface.setName(networkName);
        AccessConfig accessConfig = new AccessConfig();
        accessConfig.setName(networkName);
        accessConfig.setType("ONE_TO_ONE_NAT");
        if (instanceGroup.isPresent() && isGateway(instanceGroup.orNull().getInstanceGroupType())) {
            Compute.Addresses.Get getReservedIp = contextObject.getCompute().addresses().get(contextObject.getProjectId(), gccZone.getRegion(),
                    filterResourcesByType(resources, ResourceType.GCC_RESERVED_IP).get(0).getResourceName());
            accessConfig.setNatIP(getReservedIp.execute().getAddress());
        }
        iface.setAccessConfigs(ImmutableList.of(accessConfig));
        iface.setNetwork(String.format("https://www.googleapis.com/compute/v1/projects/%s/global/networks/%s", contextObject.getProjectId(), networkName));
        return Arrays.asList(iface);
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCC_INSTANCE;
    }

    @Override
    public Boolean stop(GccStartStopContextObject startStopContextObject, Resource resource, String region) {
        GccCredential credential = (GccCredential) startStopContextObject.getStack().getCredential();
        try {
            Compute.Instances.Stop stop = startStopContextObject.getCompute().instances()
                    .stop(credential.getProjectId(), GccZone.valueOf(region).getValue(), resource.getResourceName());
            stop.setPrettyPrint(Boolean.TRUE);
            return setInstanceState(stop.execute(), startStopContextObject, resource, credential, false);
        } catch (IOException e) {
            LOGGER.error(String.format("There was an error in the vm stop [%s]: %s", resource.getResourceName(), e.getMessage()));
            return false;
        }
    }

    @Override
    public Boolean start(GccStartStopContextObject startStopContextObject, Resource resource, String region) {
        GccCredential credential = (GccCredential) startStopContextObject.getStack().getCredential();
        try {
            Compute.Instances.Start start = startStopContextObject.getCompute().instances()
                    .start(credential.getProjectId(), GccZone.valueOf(region).getValue(), resource.getResourceName());
            start.setPrettyPrint(Boolean.TRUE);
            return setInstanceState(start.execute(), startStopContextObject, resource, credential, true);
        } catch (IOException e) {
            LOGGER.error(String.format("There was an error in the vm start [%s]: %s", resource.getResourceName(), e.getMessage()));
            return false;
        }
    }

    private Boolean setInstanceState(Operation operation, GccStartStopContextObject startStopContextObject, Resource resource,
            GccCredential credential, boolean start) throws IOException {
        if (operation.getHttpErrorStatusCode() == null) {
            Compute.ZoneOperations.Get zoneOperations = createZoneOperations(
                    startStopContextObject.getCompute(),
                    credential,
                    operation,
                    GccZone.valueOf(startStopContextObject.getStack().getRegion()));
            GccResourceReadyPollerObject instReady = new GccResourceReadyPollerObject(zoneOperations, startStopContextObject.getStack(),
                    resource.getResourceName(), operation.getName(), ResourceType.GCC_INSTANCE);
            gccInstanceReadyPollerObjectPollingService.pollWithTimeout(gccResourceCheckerStatus, instReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
            if (start) {
                updateInstanceMetadata(startStopContextObject, resource);
            }
            return true;
        } else {
            return false;
        }
    }

    private void updateInstanceMetadata(GccStartStopContextObject startStopContextObject, Resource resource) {
        Instance instance = gccStackUtil.getInstance(startStopContextObject.getStack(), startStopContextObject.getCompute(), resource);
        if (instance != null) {
            InstanceMetaData metaData = instanceMetaDataRepository.findByInstanceId(resource.getResourceName());
            Stack stack = startStopContextObject.getStack();
            String publicIP = instance.getNetworkInterfaces().get(0).getAccessConfigs().get(0).getNatIP();
            if (metaData.getAmbariServer()) {
                stack.setAmbariIp(publicIP);
                Cluster cluster = clusterRepository.findOneWithLists(stack.getCluster().getId());
                stack.setCluster(cluster);
                stackRepository.save(stack);
            }
            metaData.setPublicIp(publicIP);
            instanceMetaDataRepository.save(metaData);
        } else {
            LOGGER.error(String.format("Can't find instance by resource name (instance id) : %s", resource.getResourceName()));
        }
    }

    public class GccInstanceCreateRequest extends CreateResourceRequest {

        private Long stackId;
        private List<Resource> resources = new ArrayList<>();
        private Instance instance;
        private String projectId;
        private Compute compute;
        private GccTemplate gccTemplate;
        private GccCredential gccCredential;

        public GccInstanceCreateRequest(Long stackId, List<Resource> resources, Instance instance,
                String projectId, Compute compute, GccTemplate gccTemplate, GccCredential gccCredential, List<Resource> buildNames) {
            super(buildNames);
            this.stackId = stackId;
            this.resources = resources;
            this.instance = instance;
            this.projectId = projectId;
            this.compute = compute;
            this.gccTemplate = gccTemplate;
            this.gccCredential = gccCredential;
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

        public GccTemplate getGccTemplate() {
            return gccTemplate;
        }

        public GccCredential getGccCredential() {
            return gccCredential;
        }
    }

}
