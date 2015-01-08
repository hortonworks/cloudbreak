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
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccRemoveCheckerStatus;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccRemoveReadyPollerObject;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccResourceCheckerStatus;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccResourceCreationException;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccResourceReadyPollerObject;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccDiskMode;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccDiskType;
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
    public Boolean create(final CreateResourceRequest cRR) throws Exception {
        final GccInstanceCreateRequest gICR = (GccInstanceCreateRequest) cRR;
        Stack stack = stackRepository.findById(gICR.getStackId());
        Compute.Instances.Insert ins =
                gICR.getCompute().instances().insert(gICR.getProjectId(), gICR.getGccTemplate().getGccZone().getValue(), gICR.getInstance());
        ins.setPrettyPrint(Boolean.TRUE);
        Operation execute = ins.execute();
        if (execute.getHttpErrorStatusCode() == null) {
            Compute.ZoneOperations.Get zoneOperations = createZoneOperations(gICR.getCompute(), gICR.getGccCredential(), gICR.getGccTemplate(), execute);
            GccResourceReadyPollerObject instReady = new GccResourceReadyPollerObject(zoneOperations, stack, gICR.getInstance().getName(), execute.getName());
            gccInstanceReadyPollerObjectPollingService.pollWithTimeout(gccResourceCheckerStatus, instReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
            return true;
        } else {
            throw new GccResourceCreationException(execute.getHttpErrorMessage(), resourceType(), gICR.getInstance().getName());
        }
    }

    private List<AttachedDisk> getAttachedDisks(List<Resource> resources, GccCredential gccCredential, GccTemplate gccTemplate) {
        List<AttachedDisk> listOfDisks = new ArrayList<>();
        for (Resource resource : filterResourcesByType(resources, ResourceType.GCC_ATTACHED_DISK)) {
            AttachedDisk attachedDisk = new AttachedDisk();
            attachedDisk.setBoot(false);
            attachedDisk.setType(GccDiskType.PERSISTENT.getValue());
            attachedDisk.setMode(GccDiskMode.READ_WRITE.getValue());
            attachedDisk.setDeviceName(resource.getResourceName());
            attachedDisk.setSource(String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/disks/%s",
                    gccCredential.getProjectId(), gccTemplate.getGccZone().getValue(), resource.getResourceName()));
            listOfDisks.add(attachedDisk);
        }
        return listOfDisks;
    }

    private List<AttachedDisk> getBootDiskList(List<Resource> resources, GccCredential gccCredential, GccTemplate gccTemplate) {
        List<AttachedDisk> listOfDisks = new ArrayList<>();
        for (Resource resource : filterResourcesByType(resources, ResourceType.GCC_DISK)) {
            AttachedDisk attachedDisk = new AttachedDisk();
            attachedDisk.setBoot(true);
            attachedDisk.setType(GccDiskType.PERSISTENT.getValue());
            attachedDisk.setMode(GccDiskMode.READ_WRITE.getValue());
            attachedDisk.setDeviceName(resource.getResourceName());
            attachedDisk.setSource(String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/disks/%s",
                    gccCredential.getProjectId(), gccTemplate.getGccZone().getValue(), resource.getResourceName()));
            listOfDisks.add(attachedDisk);
        }
        return listOfDisks;
    }

    @Override
    public Boolean delete(Resource resource, GccDeleteContextObject d) throws Exception {
        Stack stack = stackRepository.findById(d.getStackId());
        try {
            GccTemplate gccTemplate = (GccTemplate) stack.getTemplate();
            GccCredential gccCredential = (GccCredential) stack.getCredential();
            Operation operation = d.getCompute().instances()
                    .delete(gccCredential.getProjectId(), gccTemplate.getGccZone().getValue(), resource.getResourceName()).execute();
            Compute.ZoneOperations.Get zoneOperations = createZoneOperations(d.getCompute(), gccCredential, gccTemplate, operation);
            Compute.GlobalOperations.Get globalOperations = createGlobalOperations(d.getCompute(), gccCredential, gccTemplate, operation);
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
    public Optional<String> describe(Resource resource, GccDescribeContextObject dco) throws Exception {
        try {
            Stack stack = stackRepository.findById(dco.getStackId());
            return Optional.fromNullable(describe(stack, dco.getCompute(), resource).toPrettyString());
        } catch (IOException e) {
            return Optional.fromNullable(jsonHelper.createJsonFromString(String.format("{\"VirtualMachine\": {%s}}", ERROR)).toString());
        }
    }

    @Override
    public List<Resource> buildNames(GccProvisionContextObject po, int index, List<Resource> resources) {
        Stack stack = stackRepository.findById(po.getStackId());
        Resource resource = new Resource(resourceType(), String.format("%s-%s-%s", stack.getName(), index, new Date().getTime()), stack);
        return Arrays.asList(resource);
    }

    @Override
    public CreateResourceRequest buildCreateRequest(GccProvisionContextObject po, List<Resource> res, List<Resource> buildNames, int index) throws Exception {
        Stack stack = stackRepository.findById(po.getStackId());
        GccCredential gccCredential = (GccCredential) stack.getCredential();
        GccTemplate gccTemplate = (GccTemplate) stack.getTemplate();

        List<AttachedDisk> listOfDisks = new ArrayList<>();
        listOfDisks.addAll(getBootDiskList(res, gccCredential, gccTemplate));
        listOfDisks.addAll(getAttachedDisks(res, gccCredential, gccTemplate));

        Instance instance = new Instance();
        instance.setMachineType(String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/machineTypes/%s",
                po.getProjectId(), gccTemplate.getGccZone().getValue(), gccTemplate.getGccInstanceType().getValue()));
        instance.setName(buildNames.get(0).getResourceName());
        instance.setCanIpForward(Boolean.TRUE);
        instance.setNetworkInterfaces(getNetworkInterface(po.getProjectId(), stack.getName()));
        instance.setDisks(listOfDisks);
        Metadata metadata = new Metadata();
        metadata.setItems(Lists.<Metadata.Items>newArrayList());

        Metadata.Items sshMetaData = new Metadata.Items();
        sshMetaData.setKey("sshKeys");
        sshMetaData.setValue("ubuntu:" + gccCredential.getPublicKey());

        Metadata.Items startupScript = new Metadata.Items();
        startupScript.setKey("startup-script");
        startupScript.setValue(po.getUserData());

        metadata.getItems().add(sshMetaData);
        metadata.getItems().add(startupScript);
        instance.setMetadata(metadata);

        return new GccInstanceCreateRequest(po.getStackId(), res, instance, po.getProjectId(), po.getCompute(), gccTemplate, gccCredential, buildNames);
    }

    public Instance describe(Stack stack, Compute compute, Resource resource) throws IOException {
        GccTemplate gccTemplate = (GccTemplate) stack.getTemplate();
        GccCredential gccCredential = (GccCredential) stack.getCredential();
        Compute.Instances.Get getVm = compute.instances().get(gccCredential.getProjectId(), gccTemplate.getGccZone().getValue(),
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
