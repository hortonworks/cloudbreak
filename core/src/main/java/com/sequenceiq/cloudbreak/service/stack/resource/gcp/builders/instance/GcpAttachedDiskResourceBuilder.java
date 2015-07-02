package com.sequenceiq.cloudbreak.service.stack.resource.gcp.builders.instance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.AsyncTaskExecutor;
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
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceNameService;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.GcpSimpleInstanceResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.model.GcpDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.model.GcpProvisionContextObject;

@Component
@Order(2)
public class GcpAttachedDiskResourceBuilder extends GcpSimpleInstanceResourceBuilder {

    @Inject
    private StackRepository stackRepository;

    @Inject
    private GcpResourceCheckerStatus gcpResourceCheckerStatus;

    @Inject
    private PollingService<GcpResourceReadyPollerObject> gcpDiskReadyPollerObjectPollingService;

    @Inject
    @Qualifier("intermediateBuilderExecutor")
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Inject
    @Named("GcpResourceNameService")
    private ResourceNameService resourceNameService;

    @Override
    public Boolean create(final CreateResourceRequest createResourceRequest, final String region) throws Exception {
        final GcpAttachedDiskCreateRequest gADCR = (GcpAttachedDiskCreateRequest) createResourceRequest;
        final Stack stack = stackRepository.findById(gADCR.getStackId());
        List<Future<Boolean>> futures = new ArrayList<>();
        final Map<String, String> mdcCtxMap = MDC.getCopyOfContextMap();
        for (final Disk disk : gADCR.getDisks()) {
            Future<Boolean> submit = intermediateBuilderExecutor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    MDC.setContextMap(mdcCtxMap);
                    Compute.Disks.Insert insDisk = gADCR.getCompute().disks().insert(gADCR.getProjectId(),
                            CloudRegion.valueOf(stack.getRegion()).value(), disk);
                    Operation execute = insDisk.execute();
                    if (execute.getHttpErrorStatusCode() == null) {
                        Compute.ZoneOperations.Get zoneOperations =
                                createZoneOperations(gADCR.getCompute(), gADCR.getGcpCredential(), execute, CloudRegion.valueOf(stack.getRegion()));
                        GcpResourceReadyPollerObject gcpDiskReady =
                                new GcpResourceReadyPollerObject(zoneOperations, stack, disk.getName(), execute.getName(), ResourceType.GCP_ATTACHED_DISK);
                        gcpDiskReadyPollerObjectPollingService.pollWithTimeout(gcpResourceCheckerStatus, gcpDiskReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
                        return true;
                    } else {
                        throw new GcpResourceException(execute.getHttpErrorMessage(), resourceType(), disk.getName());
                    }
                }
            });
            futures.add(submit);
        }
        for (Future<Boolean> future : futures) {
            future.get();
        }
        return true;
    }

    @Override
    public Boolean delete(Resource resource, GcpDeleteContextObject deleteContextObject, String region) throws Exception {
        return true;
    }

    @Override
    public List<Resource> buildResources(GcpProvisionContextObject provisionContextObject, int index, List<Resource> resources,
            Optional<InstanceGroup> instanceGroup) {
        List<Resource> names = new ArrayList<>();
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        for (int i = 0; i < instanceGroup.orNull().getTemplate().getVolumeCount(); i++) {
            String resourceName = resourceNameService.resourceName(resourceType(), stack.getName(), instanceGroup.orNull().getGroupName(), index, i);
            names.add(new Resource(resourceType(), resourceName, stack, instanceGroup.orNull().getGroupName()));
        }
        return names;
    }

    @Override
    public CreateResourceRequest buildCreateRequest(GcpProvisionContextObject provisionContextObject, List<Resource> resources,
            List<Resource> buildResources, int index, Optional<InstanceGroup> instanceGroup, Optional<String> userData) throws Exception {
        List<Disk> disks = new ArrayList<>();
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        GcpTemplate gcpTemplate = (GcpTemplate) instanceGroup.orNull().getTemplate();
        GcpCredential gcpCredential = (GcpCredential) stack.getCredential();
        for (Resource buildName : buildResources) {
            Disk disk = new Disk();
            disk.setSizeGb(instanceGroup.orNull().getTemplate().getVolumeSize().longValue());
            disk.setName(buildName.getResourceName());
            disk.setKind(gcpTemplate.getGcpRawDiskType().getUrl(provisionContextObject.getProjectId(), CloudRegion.valueOf(stack.getRegion())));
            disks.add(disk);
        }
        return new GcpAttachedDiskCreateRequest(provisionContextObject.getStackId(), resources, disks, provisionContextObject.getProjectId(),
                provisionContextObject.getCompute(), gcpTemplate, gcpCredential, buildResources);
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCP_ATTACHED_DISK;
    }

    public class GcpAttachedDiskCreateRequest extends CreateResourceRequest {

        private Long stackId;
        private List<Resource> resources = new ArrayList<>();
        private List<Disk> disks = new ArrayList<>();
        private String projectId;
        private Compute compute;
        private GcpTemplate gcpTemplate;
        private GcpCredential gcpCredential;

        public GcpAttachedDiskCreateRequest(Long stackId, List<Resource> resources, List<Disk> disks,
                String projectId, Compute compute, GcpTemplate gcpTemplate, GcpCredential gcpCredential, List<Resource> buildNames) {
            super(buildNames);
            this.stackId = stackId;
            this.resources = resources;
            this.disks = disks;
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

        public List<Disk> getDisks() {
            return disks;
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
