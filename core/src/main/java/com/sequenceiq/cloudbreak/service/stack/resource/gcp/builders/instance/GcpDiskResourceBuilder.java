package com.sequenceiq.cloudbreak.service.stack.resource.gcp.builders.instance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.Operation;
import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.domain.CloudRegion;
import com.sequenceiq.cloudbreak.domain.GcpCredential;
import com.sequenceiq.cloudbreak.domain.GcpTemplate;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.gcp.GcpResourceCheckerStatus;
import com.sequenceiq.cloudbreak.service.stack.connector.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.service.stack.connector.gcp.GcpResourceReadyPollerObject;
import com.sequenceiq.cloudbreak.service.stack.connector.gcp.GcpStackUtil;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceNameService;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.GcpSimpleInstanceResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.model.GcpDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.model.GcpProvisionContextObject;

@Component
@Order(1)
public class GcpDiskResourceBuilder extends GcpSimpleInstanceResourceBuilder {

    private static final long SIZE = 50L;

    @Inject
    private StackRepository stackRepository;
    @Inject
    private GcpResourceCheckerStatus gcpResourceCheckerStatus;
    @Inject
    private PollingService<GcpResourceReadyPollerObject> gcpDiskReadyPollerObjectPollingService;
    @Inject
    private GcpStackUtil gcpStackUtil;
    @Inject
    @Named("GcpResourceNameService")
    private ResourceNameService resourceNameService;

    @Override
    public Boolean create(final CreateResourceRequest createResourceRequest, String region) throws Exception {
        final GcpDiskCreateRequest gDCR = (GcpDiskCreateRequest) createResourceRequest;
        Stack stack = stackRepository.findById(gDCR.getStackId());
        Compute.Disks.Insert insDisk = gDCR.getCompute().disks().insert(gDCR.getProjectId(), CloudRegion.valueOf(stack.getRegion()).value(), gDCR.getDisk());
        insDisk.setSourceImage(gcpStackUtil.getAmbariUbuntu(gDCR.getProjectId(), stack.getImage()));
        Operation execute = insDisk.execute();
        if (execute.getHttpErrorStatusCode() == null) {
            Compute.ZoneOperations.Get zoneOperations = createZoneOperations(gDCR.getCompute(),
                    gDCR.getGcpCredential(), execute, CloudRegion.valueOf(stack.getRegion()));
            GcpResourceReadyPollerObject gcpDiskReady =
                    new GcpResourceReadyPollerObject(zoneOperations, stack, gDCR.getDisk().getName(), execute.getName(), ResourceType.GCP_DISK);
            gcpDiskReadyPollerObjectPollingService.pollWithTimeout(gcpResourceCheckerStatus, gcpDiskReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
            return true;
        } else {
            throw new GcpResourceException(execute.getHttpErrorMessage(), resourceType(), gDCR.getDisk().getName());
        }
    }

    @Override
    public Boolean delete(Resource resource, GcpDeleteContextObject deleteContextObject, String region) throws Exception {
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
        Disk disk = new Disk();
        disk.setSizeGb(SIZE);
        disk.setName(buildResources.get(0).getResourceName());
        disk.setKind(gcpTemplate.getGcpRawDiskType().getUrl(provisionContextObject.getProjectId(), CloudRegion.valueOf(stack.getRegion())));
        return new GcpDiskCreateRequest(provisionContextObject.getStackId(), resources, disk, provisionContextObject.getProjectId(),
                provisionContextObject.getCompute(), gcpTemplate, gcpCredential, buildResources);
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCP_DISK;
    }

    public class GcpDiskCreateRequest extends CreateResourceRequest {

        private Long stackId;
        private List<Resource> resources = new ArrayList<>();
        private Disk disk;
        private String projectId;
        private Compute compute;
        private GcpTemplate gcpTemplate;
        private GcpCredential gcpCredential;

        public GcpDiskCreateRequest(Long stackId, List<Resource> resources, Disk disk,
                String projectId, Compute compute, GcpTemplate gcpTemplate, GcpCredential gcpCredential, List<Resource> buildNames) {
            super(buildNames);
            this.stackId = stackId;
            this.resources = resources;
            this.disk = disk;
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

        public Disk getDisk() {
            return disk;
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
