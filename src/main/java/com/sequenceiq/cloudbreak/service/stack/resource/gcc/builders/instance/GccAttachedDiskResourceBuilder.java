package com.sequenceiq.cloudbreak.service.stack.resource.gcc.builders.instance;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.ERROR;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.Operation;
import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.domain.GccTemplate;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccDiskCheckerStatus;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccDiskMode;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccDiskReadyPollerObject;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccDiskType;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccRemoveCheckerStatus;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccRemoveReadyPollerObject;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccZone;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.GccSimpleInstanceResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccDescribeContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccProvisionContextObject;

@Component
@Order(2)
public class GccAttachedDiskResourceBuilder extends GccSimpleInstanceResourceBuilder {

    @Autowired
    private StackRepository stackRepository;
    @Autowired
    private GccDiskCheckerStatus gccDiskCheckerStatus;
    @Autowired
    private PollingService<GccDiskReadyPollerObject> gccDiskReadyPollerObjectPollingService;
    @Autowired
    private GccRemoveCheckerStatus gccRemoveCheckerStatus;
    @Autowired
    private PollingService<GccRemoveReadyPollerObject> gccRemoveReadyPollerObjectPollingService;
    @Autowired
    private JsonHelper jsonHelper;
    @Override
    public List<Resource> create(GccProvisionContextObject po) throws Exception {
        return create(po, 0);
    }

    @Override
    public List<Resource> create(GccProvisionContextObject po, int index) throws Exception {
        Stack stack = stackRepository.findById(po.getStackId());
        GccTemplate gccTemplate = (GccTemplate) stack.getTemplate();
        GccCredential gccCredential = (GccCredential) stack.getCredential();
        List<Resource> resources = new ArrayList<>();
        List<AttachedDisk> listOfDisks = new ArrayList<>();
        String name = String.format("%s-%s", stack.getName(), index);
        AttachedDisk diskToInsert = new AttachedDisk();
        diskToInsert.setBoot(true);
        diskToInsert.setType(GccDiskType.PERSISTENT.getValue());
        diskToInsert.setMode(GccDiskMode.READ_WRITE.getValue());
        diskToInsert.setDeviceName(name);
        diskToInsert.setSource(String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/disks/%s?sourceImage=%s",
                gccCredential.getProjectId(), gccTemplate.getGccZone().getValue(), name, gccTemplate.getGccZone()));
        listOfDisks.add(diskToInsert);

        for (int i = 0; i < gccTemplate.getVolumeCount(); i++) {
            String value = name + "-" + i;
            resources.add(new Resource(resourceType(), value, stack));
            Disk disk1 = buildRawDisk(po.getCompute(), stack, gccCredential.getProjectId(),
                    gccTemplate.getGccZone(), value, Long.parseLong(gccTemplate.getVolumeSize().toString()));
            AttachedDisk diskToInsert1 = new AttachedDisk();
            diskToInsert1.setBoot(false);
            diskToInsert1.setType(GccDiskType.PERSISTENT.getValue());
            diskToInsert1.setMode(GccDiskMode.READ_WRITE.getValue());
            diskToInsert1.setDeviceName(value);
            diskToInsert1.setSource(String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/disks/%s",
                    gccCredential.getProjectId(), gccTemplate.getGccZone().getValue(), disk1.getName()));
            listOfDisks.add(diskToInsert1);
        }
        List<AttachedDisk> diskList = po.getDiskList(name);
        diskList.addAll(listOfDisks);
        po.withDiskList(name, diskList);
        return resources;
    }

    private Disk buildRawDisk(Compute compute, Stack stack, String projectId, GccZone zone, String name, Long size) throws IOException {
        Disk disk = new Disk();
        disk.setSizeGb(size.longValue());
        disk.setName(name);
        disk.setKind(((GccTemplate) stack.getTemplate()).getGccRawDiskType().getUrl(projectId, zone));
        Compute.Disks.Insert insDisk = compute.disks().insert(projectId, zone.getValue(), disk);
        insDisk.execute();
        GccDiskReadyPollerObject gccDiskReady = new GccDiskReadyPollerObject(compute, stack, name);
        gccDiskReadyPollerObjectPollingService.pollWithTimeout(gccDiskCheckerStatus, gccDiskReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        return disk;
    }

    @Override
    public Boolean delete(Resource resource, GccDeleteContextObject d) throws Exception {
        try {
            Stack stack = stackRepository.findById(d.getStackId());
            GccTemplate gccTemplate = (GccTemplate) stack.getTemplate();
            GccCredential gccCredential = (GccCredential) stack.getCredential();
            Operation execute = d.getCompute().disks()
                    .delete(gccCredential.getProjectId(), gccTemplate.getGccZone().getValue(), resource.getResourceName()).execute();
            Compute.ZoneOperations.Get zoneOperations = createZoneOperations(d.getCompute(), gccCredential, gccTemplate, execute);
            Compute.GlobalOperations.Get globalOperations = createGlobalOperations(d.getCompute(), gccCredential, gccTemplate, execute);
            GccRemoveReadyPollerObject gccRemoveReady =
                    new GccRemoveReadyPollerObject(zoneOperations, globalOperations, stack.getId(), resource.getResourceName());
            gccRemoveReadyPollerObjectPollingService.pollWithTimeout(gccRemoveCheckerStatus, gccRemoveReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        } catch (GoogleJsonResponseException ex) {
            exceptionHandler(ex, resource.getResourceName());
        } catch (IOException e) {
            throw new InternalServerException(e.getMessage());
        }
        return true;
    }

    @Override
    public Optional<String> describe(Resource resource, GccDescribeContextObject dco) throws Exception {
        Stack stack = stackRepository.findById(dco.getStackId());
        GccTemplate gccTemplate = (GccTemplate) stack.getTemplate();
        GccCredential gccCredential = (GccCredential) stack.getCredential();
        try {
            Compute.Disks.Get getDisk =
                    dco.getCompute().disks().get(gccCredential.getProjectId(), gccTemplate.getGccZone().getValue(), resource.getResourceName());
            return Optional.fromNullable(getDisk.execute().toPrettyString());
        } catch (IOException e) {
            return Optional.fromNullable(jsonHelper.createJsonFromString(String.format("{\"Attached_Disk\": {%s}}", ERROR)).toString());
        }
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCC_ATTACHED_DISK;
    }

}
