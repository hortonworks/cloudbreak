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
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccRemoveCheckerStatus;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccRemoveReadyPollerObject;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccResourceCheckerStatus;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccResourceCreationException;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccResourceReadyPollerObject;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccStackUtil;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
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
    private GccResourceCheckerStatus gccResourceCheckerStatus;
    @Autowired
    private PollingService<GccResourceReadyPollerObject> gccDiskReadyPollerObjectPollingService;
    @Autowired
    private GccRemoveCheckerStatus gccRemoveCheckerStatus;
    @Autowired
    private PollingService<GccRemoveReadyPollerObject> gccRemoveReadyPollerObjectPollingService;
    @Autowired
    private JsonHelper jsonHelper;
    @Autowired
    private GccStackUtil gccStackUtil;

    @Override
    public Boolean create(final CreateResourceRequest cRR) throws Exception {
        final GccDiskCreateRequest gDCR = (GccDiskCreateRequest) cRR;
        Stack stack = stackRepository.findById(gDCR.getStackId());
        Compute.Disks.Insert insDisk = gDCR.getCompute().disks().insert(gDCR.getProjectId(), gDCR.getGccTemplate().getGccZone().getValue(), gDCR.getDisk());
        insDisk.setSourceImage(gccStackUtil.getAmbariUbuntu(gDCR.getProjectId()));
        Operation execute = insDisk.execute();
        if (execute.getHttpErrorStatusCode() == null) {
            Compute.ZoneOperations.Get zoneOperations = createZoneOperations(gDCR.getCompute(), gDCR.getGccCredential(), gDCR.gccTemplate, execute);
            GccResourceReadyPollerObject gccDiskReady = new GccResourceReadyPollerObject(zoneOperations, stack, gDCR.getDisk().getName(), execute.getName());
            gccDiskReadyPollerObjectPollingService.pollWithTimeout(gccResourceCheckerStatus, gccDiskReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
            return true;
        } else {
            throw new GccResourceCreationException(execute.getHttpErrorMessage(), resourceType(), gDCR.getDisk().getName());
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
        Disk disk = new Disk();
        disk.setSizeGb(SIZE);
        disk.setName(buildNames.get(0).getResourceName());
        disk.setKind(gccTemplate.getGccRawDiskType().getUrl(po.getProjectId(), gccTemplate.getGccZone()));
        return new GccDiskCreateRequest(po.getStackId(), res, disk, po.getProjectId(), po.getCompute(), gccTemplate, gccCredential, buildNames);
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCC_DISK;
    }

    public class GccDiskCreateRequest extends CreateResourceRequest {

        private Long stackId;
        private List<Resource> resources = new ArrayList<>();
        private Disk disk;
        private String projectId;
        private Compute compute;
        private GccTemplate gccTemplate;
        private GccCredential gccCredential;

        public GccDiskCreateRequest(Long stackId, List<Resource> resources, Disk disk,
                String projectId, Compute compute, GccTemplate gccTemplate, GccCredential gccCredential, List<Resource> buildNames) {
            super(buildNames);
            this.stackId = stackId;
            this.resources = resources;
            this.disk = disk;
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

        public Disk getDisk() {
            return disk;
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
