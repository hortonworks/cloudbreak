package com.sequenceiq.cloudbreak.service.stack.resource.gcc.builders.instance;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.ERROR;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
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
import com.sequenceiq.cloudbreak.service.StackDependentPollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccRemoveCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccRemoveReadyPollerObject;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccResourceCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccResourceCreationException;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccResourceReadyPollerObject;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccStackUtil;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.GccSimpleInstanceResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccDescribeContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccProvisionContextObject;

@Component
@Order(1)
public class GccDiskResourceBuilder extends GccSimpleInstanceResourceBuilder {
    private static final long SIZE = 20L;

    @Autowired
    private StackRepository stackRepository;
    @Autowired
    private GccResourceCheckerTask gccResourceCheckerTask;
    @Autowired
    private StackDependentPollingService<GccResourceReadyPollerObject> gccDiskReadyPollerObjectPollingService;
    @Autowired
    private GccRemoveCheckerTask gccRemoveCheckerTask;
    @Autowired
    private StackDependentPollingService<GccRemoveReadyPollerObject> gccRemoveReadyPollerObjectPollingService;
    @Autowired
    private JsonHelper jsonHelper;
    @Autowired
    private GccStackUtil gccStackUtil;

    @Override
    public List<Resource> create(GccProvisionContextObject po, int index, List<Resource> resources) throws Exception {
        Stack stack = stackRepository.findById(po.getStackId());
        GccTemplate template = (GccTemplate) stack.getTemplate();
        GccCredential credential = (GccCredential) stack.getCredential();
        String name = String.format("%s-%s-%s", stack.getName(), index, new Date().getTime());
        Disk disk = new Disk();
        disk.setSizeGb(SIZE);
        disk.setName(name);
        disk.setKind(((GccTemplate) stack.getTemplate()).getGccRawDiskType().getUrl(po.getProjectId(), template.getGccZone()));
        Compute.Disks.Insert insDisk = po.getCompute().disks().insert(po.getProjectId(), template.getGccZone().getValue(), disk);
        insDisk.setSourceImage(gccStackUtil.getAmbariUbuntu(po.getProjectId()));
        Operation execute = insDisk.execute();
        if (execute.getHttpErrorStatusCode() == null) {
            Compute.ZoneOperations.Get zoneOperations = createZoneOperations(po.getCompute(), credential, template, execute);
            GccResourceReadyPollerObject gccDiskReady = new GccResourceReadyPollerObject(zoneOperations, stack, name, execute.getName());
            gccDiskReadyPollerObjectPollingService.pollWithTimeout(gccResourceCheckerTask, gccDiskReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
            return Arrays.asList(new Resource(resourceType(), name, stack));
        } else {
            throw new GccResourceCreationException(execute.getHttpErrorMessage(), resourceType(), name);
        }
    }

    @Override
    public Boolean delete(Resource resource, GccDeleteContextObject d) throws Exception {
        Stack stack = stackRepository.findById(d.getStackId());
        try {
            GccTemplate gccTemplate = (GccTemplate) stack.getTemplate();
            GccCredential gccCredential = (GccCredential) stack.getCredential();
            Operation operation = d.getCompute().disks()
                    .delete(gccCredential.getProjectId(), gccTemplate.getGccZone().getValue(), resource.getResourceName()).execute();
            Compute.ZoneOperations.Get zoneOperations = createZoneOperations(d.getCompute(), gccCredential, gccTemplate, operation);
            Compute.GlobalOperations.Get globalOperations = createGlobalOperations(d.getCompute(), gccCredential, gccTemplate, operation);
            GccRemoveReadyPollerObject gccRemoveReady =
                    new GccRemoveReadyPollerObject(zoneOperations, globalOperations, stack, resource.getResourceName(), operation.getName());
            gccRemoveReadyPollerObjectPollingService.pollWithTimeout(gccRemoveCheckerTask, gccRemoveReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        } catch (GoogleJsonResponseException ex) {
            exceptionHandler(ex, resource.getResourceName(), stack);
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
            return Optional.fromNullable(jsonHelper.createJsonFromString(String.format("{\"Disk\": {%s}}", ERROR)).toString());
        }
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCC_DISK;
    }

}
