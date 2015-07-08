package com.sequenceiq.cloudbreak.service.stack.resource.gcp.builders.network;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Address;
import com.google.api.services.compute.model.Operation;
import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.domain.CloudRegion;
import com.sequenceiq.cloudbreak.domain.GcpCredential;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.gcp.GcpRemoveCheckerStatus;
import com.sequenceiq.cloudbreak.service.stack.connector.gcp.GcpRemoveReadyPollerObject;
import com.sequenceiq.cloudbreak.service.stack.connector.gcp.GcpResourceCheckerStatus;
import com.sequenceiq.cloudbreak.service.stack.connector.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.service.stack.connector.gcp.GcpResourceReadyPollerObject;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceNameService;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.GcpSimpleNetworkResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.model.GcpDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.model.GcpProvisionContextObject;

@Component
@Order(4)
public class GcpReservedIpResourceBuilder extends GcpSimpleNetworkResourceBuilder {

    @Inject
    private StackRepository stackRepository;
    @Inject
    private GcpResourceCheckerStatus gcpResourceCheckerStatus;
    @Inject
    private PollingService<GcpResourceReadyPollerObject> gcpReservedIpReadyPollerObjectPollingService;
    @Inject
    private GcpRemoveCheckerStatus gcpRemoveCheckerStatus;
    @Inject
    private PollingService<GcpRemoveReadyPollerObject> gcpRemoveReadyPollerObjectPollingService;
    @Inject
    @Named("GcpResourceNameService")
    private ResourceNameService resourceNameService;

    @Override
    public Boolean create(CreateResourceRequest createResourceRequest, String region) throws Exception {
        final GcpReservedIpCreateRequest reservedIpCreateRequest = (GcpReservedIpCreateRequest) createResourceRequest;
        Stack stack = stackRepository.findById(reservedIpCreateRequest.getStackId());
        Compute.Addresses.Insert networkInsert = reservedIpCreateRequest
                .getCompute()
                .addresses()
                .insert(reservedIpCreateRequest.getProjectId(), reservedIpCreateRequest.getGcpZone().region(), reservedIpCreateRequest.getAddress());
        Operation execute = networkInsert.execute();
        if (execute.getHttpErrorStatusCode() == null) {
            Compute.RegionOperations.Get regionOperations = createRegionOperations(reservedIpCreateRequest.getCompute(),
                    reservedIpCreateRequest.getGcpCredential(),
                    execute,
                    reservedIpCreateRequest.getGcpZone());
            GcpResourceReadyPollerObject instReady =
                    new GcpResourceReadyPollerObject(
                            regionOperations,
                            stack,
                            reservedIpCreateRequest.getAddress().getName(),
                            execute.getName(),
                            ResourceType.GCP_RESERVED_IP);
            gcpReservedIpReadyPollerObjectPollingService.pollWithTimeout(gcpResourceCheckerStatus, instReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        } else {
            throw new GcpResourceException(execute.getHttpErrorMessage(), resourceType(), reservedIpCreateRequest.getAddress().getName());
        }
        return true;
    }

    @Override
    public Boolean delete(Resource resource, GcpDeleteContextObject deleteContextObject, String region) throws Exception {
        Stack stack = stackRepository.findById(deleteContextObject.getStackId());
        try {
            GcpCredential gcpCredential = (GcpCredential) stack.getCredential();
            Operation operation = deleteContextObject
                    .getCompute()
                    .addresses()
                    .delete(gcpCredential.getProjectId(), CloudRegion.valueOf(region).region(), resource.getResourceName())
                    .execute();
            Compute.RegionOperations.Get regionOperations = createRegionOperations(
                    deleteContextObject.getCompute(),
                    gcpCredential,
                    operation,
                    CloudRegion.valueOf(region)
            );
            Compute.GlobalOperations.Get globalOperations = createGlobalOperations(
                    deleteContextObject.getCompute(),
                    gcpCredential,
                    operation
            );
            GcpRemoveReadyPollerObject gcpRemoveReady = new GcpRemoveReadyPollerObject(
                    regionOperations,
                    globalOperations,
                    stack,
                    resource.getResourceName(),
                    operation.getName(),
                    resourceType()
            );
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
        String resourceName = resourceNameService.resourceName(resourceType(), stack.getName());
        return Arrays.asList(new Resource(resourceType(), resourceName, stack, null));
    }

    @Override
    public CreateResourceRequest buildCreateRequest(GcpProvisionContextObject provisionContextObject, List<Resource> resources, List<Resource> buildResources,
            int index, Optional<InstanceGroup> instanceGroup, Optional<String> userData) throws Exception {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        Address address = new Address();
        address.setRegion(CloudRegion.valueOf(stack.getRegion()).region());
        address.setName(buildResources.get(0).getResourceName());
        return new GcpReservedIpCreateRequest(provisionContextObject.getStackId(), address,
                provisionContextObject.getProjectId(), provisionContextObject.getCompute(),
                CloudRegion.valueOf(stack.getRegion()), buildResources, (GcpCredential) stack.getCredential());
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCP_RESERVED_IP;
    }

    public class GcpReservedIpCreateRequest extends CreateResourceRequest {
        private Address address;
        private Long stackId;
        private String projectId;
        private Compute compute;
        private CloudRegion gcpZone;
        private GcpCredential gcpCredential;

        public GcpReservedIpCreateRequest(Long stackId, Address address, String projectId, Compute compute, CloudRegion gcpZone, List<Resource> buildNames,
                GcpCredential gcpCredential) {
            super(buildNames);
            this.stackId = stackId;
            this.address = address;
            this.projectId = projectId;
            this.compute = compute;
            this.gcpZone = gcpZone;
            this.gcpCredential = gcpCredential;
        }

        public Long getStackId() {
            return stackId;
        }

        public Address getAddress() {
            return address;
        }

        public CloudRegion getGcpZone() {
            return gcpZone;
        }

        public String getProjectId() {
            return projectId;
        }

        public Compute getCompute() {
            return compute;
        }

        public GcpCredential getGcpCredential() {
            return gcpCredential;
        }
    }
}
