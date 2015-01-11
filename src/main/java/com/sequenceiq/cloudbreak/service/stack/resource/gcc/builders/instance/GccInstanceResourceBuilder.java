package com.sequenceiq.cloudbreak.service.stack.resource.gcc.builders.instance;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.ERROR;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.domain.GccTemplate;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.TemplateGroup;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccRemoveCheckerStatus;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccRemoveReadyPollerObject;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccResourceCheckerStatus;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccResourceCreationException;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccResourceReadyPollerObject;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccDiskMode;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccDiskType;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccZone;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.GccSimpleInstanceResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccDescribeContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccProvisionContextObject;

@Component
@Order(3)
public class GccInstanceResourceBuilder extends GccSimpleInstanceResourceBuilder {

    @Autowired
    private StackRepository stackRepository;
    @Autowired
    private GccResourceCheckerStatus gccResourceCheckerStatus;
    @Autowired
    private PollingService<GccResourceReadyPollerObject> gccInstanceReadyPollerObjectPollingService;
    @Autowired
    private GccRemoveCheckerStatus gccRemoveCheckerStatus;
    @Autowired
    private PollingService<GccRemoveReadyPollerObject> gccRemoveReadyPollerObjectPollingService;
    @Autowired
    private JsonHelper jsonHelper;

    @Override
    public Boolean create(final CreateResourceRequest createResourceRequest, TemplateGroup templateGroup, String region) throws Exception {
        final GccInstanceCreateRequest gICR = (GccInstanceCreateRequest) createResourceRequest;
        Stack stack = stackRepository.findById(gICR.getStackId());
        Compute.Instances.Insert ins =
                gICR.getCompute().instances().insert(gICR.getProjectId(), GccZone.valueOf(stack.getRegion()).getValue(), gICR.getInstance());
        ins.setPrettyPrint(Boolean.TRUE);
        Operation execute = ins.execute();
        if (execute.getHttpErrorStatusCode() == null) {
            Compute.ZoneOperations.Get zoneOperations = createZoneOperations(gICR.getCompute(),
                    gICR.getGccCredential(), execute, GccZone.valueOf(stack.getRegion()));
            GccResourceReadyPollerObject instReady = new GccResourceReadyPollerObject(zoneOperations, stack, gICR.getInstance().getName(), execute.getName());
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
                    new GccRemoveReadyPollerObject(zoneOperations, globalOperations, stack, resource.getResourceName(), operation.getName());
            gccRemoveReadyPollerObjectPollingService.pollWithTimeout(gccRemoveCheckerStatus, gccRemoveReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        } catch (GoogleJsonResponseException ex) {
            exceptionHandler(ex, resource.getResourceName(), stack);
        } catch (IOException e) {
            throw new InternalServerException(e.getMessage());
        }
        return true;
    }

    @Override
    public Optional<String> describe(Resource resource, GccDescribeContextObject describeContextObject, String region) throws Exception {
        try {
            Stack stack = stackRepository.findById(describeContextObject.getStackId());
            return Optional.fromNullable(describe(stack, describeContextObject.getCompute(), resource,  GccZone.valueOf(region)).toPrettyString());
        } catch (IOException e) {
            return Optional.fromNullable(jsonHelper.createJsonFromString(String.format("{\"VirtualMachine\": {%s}}", ERROR)).toString());
        }
    }

    @Override
    public List<Resource> buildResources(GccProvisionContextObject provisionContextObject, int index, List<Resource> resources, TemplateGroup templateGroup) {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        Resource resource = new Resource(resourceType(),
                String.format("%s-%s-%s", stack.getName(), index, new Date().getTime()), stack,
                Lists.newArrayList(stack.getTemplateGroups()).get(0).getGroupName());
        return Arrays.asList(resource);
    }

    @Override
    public CreateResourceRequest buildCreateRequest(GccProvisionContextObject provisionContextObject, List<Resource> resources,
            List<Resource> buildResources, int index, TemplateGroup templateGroup) throws Exception {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        GccCredential gccCredential = (GccCredential) stack.getCredential();
        GccTemplate gccTemplate = (GccTemplate) templateGroup.getTemplate();

        List<AttachedDisk> listOfDisks = new ArrayList<>();
        listOfDisks.addAll(getBootDiskList(resources, gccCredential, GccZone.valueOf(stack.getRegion())));
        listOfDisks.addAll(getAttachedDisks(resources, gccCredential, GccZone.valueOf(stack.getRegion())));

        Instance instance = new Instance();
        instance.setMachineType(String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/machineTypes/%s",
                provisionContextObject.getProjectId(), GccZone.valueOf(stack.getRegion()).getValue(), gccTemplate.getGccInstanceType().getValue()));
        instance.setName(buildResources.get(0).getResourceName());
        instance.setCanIpForward(Boolean.TRUE);
        instance.setNetworkInterfaces(getNetworkInterface(provisionContextObject.getProjectId(), stack.getName()));
        instance.setDisks(listOfDisks);
        Metadata metadata = new Metadata();
        metadata.setItems(Lists.<Metadata.Items>newArrayList());

        Metadata.Items sshMetaData = new Metadata.Items();
        sshMetaData.setKey("sshKeys");
        sshMetaData.setValue("ubuntu:" + gccCredential.getPublicKey());

        Metadata.Items startupScript = new Metadata.Items();
        startupScript.setKey("startup-script");
        startupScript.setValue(provisionContextObject.getUserData());

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

    private List<NetworkInterface> getNetworkInterface(String projectId, String name) {
        NetworkInterface iface = new NetworkInterface();
        iface.setName(name);
        AccessConfig accessConfig = new AccessConfig();
        accessConfig.setName(name);
        accessConfig.setType("ONE_TO_ONE_NAT");
        iface.setAccessConfigs(ImmutableList.of(accessConfig));
        iface.setNetwork(String.format("https://www.googleapis.com/compute/v1/projects/%s/global/networks/%s", projectId, name));
        return Arrays.asList(iface);
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCC_INSTANCE;
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
