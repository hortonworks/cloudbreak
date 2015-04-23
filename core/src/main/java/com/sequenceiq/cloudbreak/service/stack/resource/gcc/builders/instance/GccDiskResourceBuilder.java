package com.sequenceiq.cloudbreak.service.stack.resource.gcc.builders.instance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.Operation;
import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.domain.GccTemplate;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccRemoveReadyPollerObject;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccResourceCheckerStatus;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccResourceCreationException;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccResourceReadyPollerObject;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccStackUtil;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccZone;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.GccSimpleInstanceResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccProvisionContextObject;

@Component
@Order(1)
public class GccDiskResourceBuilder extends GccSimpleInstanceResourceBuilder {
    private static final long SIZE = 30L;

    @Autowired
    private StackRepository stackRepository;
    @Autowired
    private GccResourceCheckerStatus gccResourceCheckerStatus;
    @Autowired
    private PollingService<GccResourceReadyPollerObject> gccDiskReadyPollerObjectPollingService;
    @Autowired
    private PollingService<GccRemoveReadyPollerObject> gccRemoveReadyPollerObjectPollingService;
    @Autowired
    private JsonHelper jsonHelper;
    @Autowired
    private GccStackUtil gccStackUtil;

    @Override
    public Boolean create(final CreateResourceRequest createResourceRequest, String region) throws Exception {
        final GccDiskCreateRequest gDCR = (GccDiskCreateRequest) createResourceRequest;
        Stack stack = stackRepository.findById(gDCR.getStackId());
        Compute.Disks.Insert insDisk = gDCR.getCompute().disks().insert(gDCR.getProjectId(), GccZone.valueOf(stack.getRegion()).getValue(), gDCR.getDisk());
        insDisk.setSourceImage(gccStackUtil.getAmbariUbuntu(gDCR.getProjectId(), stack.getImage()));
        Operation execute = insDisk.execute();
        if (execute.getHttpErrorStatusCode() == null) {
            Compute.ZoneOperations.Get zoneOperations = createZoneOperations(gDCR.getCompute(),
                    gDCR.getGccCredential(), execute, GccZone.valueOf(stack.getRegion()));
            GccResourceReadyPollerObject gccDiskReady =
                    new GccResourceReadyPollerObject(zoneOperations, stack, gDCR.getDisk().getName(), execute.getName(), ResourceType.GCC_DISK);
            gccDiskReadyPollerObjectPollingService.pollWithTimeout(gccResourceCheckerStatus, gccDiskReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
            return true;
        } else {
            throw new GccResourceCreationException(execute.getHttpErrorMessage(), resourceType(), gDCR.getDisk().getName());
        }
    }

    @Override
    public Boolean delete(Resource resource, GccDeleteContextObject deleteContextObject, String region) throws Exception {
        return true;
    }

    @Override
    public List<Resource> buildResources(GccProvisionContextObject provisionContextObject, int index, List<Resource> resources,
            Optional<InstanceGroup> instanceGroup) {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        Resource resource = new Resource(resourceType(),
                String.format("%s-%s-%s", stack.getName(), index, new Date().getTime()), stack, instanceGroup.orNull().getGroupName());
        return Arrays.asList(resource);
    }

    @Override
    public CreateResourceRequest buildCreateRequest(GccProvisionContextObject provisionContextObject, List<Resource> resources,
            List<Resource> buildResources, int index, Optional<InstanceGroup> instanceGroup, Optional<String> userData) throws Exception {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        GccCredential gccCredential = (GccCredential) stack.getCredential();
        GccTemplate gccTemplate = (GccTemplate) instanceGroup.orNull().getTemplate();
        Disk disk = new Disk();
        disk.setSizeGb(SIZE);
        disk.setName(buildResources.get(0).getResourceName());
        disk.setKind(gccTemplate.getGccRawDiskType().getUrl(provisionContextObject.getProjectId(), GccZone.valueOf(stack.getRegion())));
        return new GccDiskCreateRequest(provisionContextObject.getStackId(), resources, disk, provisionContextObject.getProjectId(),
                provisionContextObject.getCompute(), gccTemplate, gccCredential, buildResources);
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
