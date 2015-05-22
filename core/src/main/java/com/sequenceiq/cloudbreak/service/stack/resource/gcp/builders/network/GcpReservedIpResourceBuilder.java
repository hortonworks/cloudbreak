package com.sequenceiq.cloudbreak.service.stack.resource.gcp.builders.network;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Address;
import com.google.api.services.compute.model.Operation;
import com.google.common.base.Optional;
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
import com.sequenceiq.cloudbreak.service.stack.connector.gcp.GcpResourceReadyPollerObject;
import com.sequenceiq.cloudbreak.service.stack.connector.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.service.stack.connector.gcp.domain.GcpZone;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.GcpSimpleNetworkResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.model.GcpDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.model.GcpProvisionContextObject;

@Component
@Order(4)
public class GcpReservedIpResourceBuilder extends GcpSimpleNetworkResourceBuilder {

    @Autowired
    private StackRepository stackRepository;
    @Autowired
    private GcpResourceCheckerStatus gcpResourceCheckerStatus;
    @Autowired
    private PollingService<GcpResourceReadyPollerObject> gcpReservedIpReadyPollerObjectPollingService;
    @Autowired
    private GcpRemoveCheckerStatus gcpRemoveCheckerStatus;
    @Autowired
    private PollingService<GcpRemoveReadyPollerObject> gcpRemoveReadyPollerObjectPollingService;

    @Override
    public Boolean create(CreateResourceRequest createResourceRequest, String region) throws Exception {
        final GcpReservedIpCreateRequest reservedIpCreateRequest = (GcpReservedIpCreateRequest) createResourceRequest;
        Stack stack = stackRepository.findById(reservedIpCreateRequest.getStackId());
        Compute.Addresses.Insert networkInsert = reservedIpCreateRequest
                .getCompute()
                .addresses()
                .insert(reservedIpCreateRequest.getProjectId(), reservedIpCreateRequest.getGcpZone().getRegion(), reservedIpCreateRequest.getAddress());
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
                    .delete(gcpCredential.getProjectId(), GcpZone.valueOf(region).getRegion(), resource.getResourceName())
                    .execute();
            Compute.RegionOperations.Get regionOperations = createRegionOperations(
                    deleteContextObject.getCompute(),
                    gcpCredential,
                    operation,
                    GcpZone.valueOf(region)
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
        return Arrays.asList(new Resource(resourceType(), getTimestampedName(stack.getName() + "reservedip"), stack, null));
    }

    @Override
    public CreateResourceRequest buildCreateRequest(GcpProvisionContextObject provisionContextObject, List<Resource> resources, List<Resource> buildResources,
            int index, Optional<InstanceGroup> instanceGroup, Optional<String> userData) throws Exception {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        Address address = new Address();
        address.setRegion(GcpZone.valueOf(stack.getRegion()).getRegion());
        address.setName(buildResources.get(0).getResourceName());
        return new GcpReservedIpCreateRequest(provisionContextObject.getStackId(), address,
                provisionContextObject.getProjectId(), provisionContextObject.getCompute(),
                GcpZone.valueOf(stack.getRegion()), buildResources, (GcpCredential) stack.getCredential());
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
        private GcpZone gcpZone;
        private GcpCredential gcpCredential;

        public GcpReservedIpCreateRequest(Long stackId, Address address, String projectId, Compute compute, GcpZone gcpZone, List<Resource> buildNames,
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

        public GcpZone getGcpZone() {
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
