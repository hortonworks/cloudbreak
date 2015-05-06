package com.sequenceiq.cloudbreak.service.stack.resource.gcc.builders.network;

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
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccRemoveCheckerStatus;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccRemoveReadyPollerObject;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccResourceCheckerStatus;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccResourceReadyPollerObject;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GcpResourceException;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccZone;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.GccSimpleNetworkResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccProvisionContextObject;

@Component
@Order(4)
public class GccReservedIpResourceBuilder extends GccSimpleNetworkResourceBuilder {

    @Autowired
    private StackRepository stackRepository;
    @Autowired
    private GccResourceCheckerStatus gccResourceCheckerStatus;
    @Autowired
    private PollingService<GccResourceReadyPollerObject> gccReservedIpReadyPollerObjectPollingService;
    @Autowired
    private GccRemoveCheckerStatus gccRemoveCheckerStatus;
    @Autowired
    private PollingService<GccRemoveReadyPollerObject> gccRemoveReadyPollerObjectPollingService;

    @Override
    public Boolean create(CreateResourceRequest createResourceRequest, String region) throws Exception {
        final GccReservedIpCreateRequest reservedIpCreateRequest = (GccReservedIpCreateRequest) createResourceRequest;
        Stack stack = stackRepository.findById(reservedIpCreateRequest.getStackId());
        Compute.Addresses.Insert networkInsert = reservedIpCreateRequest
                .getCompute()
                .addresses()
                .insert(reservedIpCreateRequest.getProjectId(), reservedIpCreateRequest.getGccZone().getRegion(), reservedIpCreateRequest.getAddress());
        Operation execute = networkInsert.execute();
        if (execute.getHttpErrorStatusCode() == null) {
            Compute.RegionOperations.Get regionOperations = createRegionOperations(reservedIpCreateRequest.getCompute(),
                    reservedIpCreateRequest.getGccCredential(),
                    execute,
                    reservedIpCreateRequest.getGccZone());
            GccResourceReadyPollerObject instReady =
                    new GccResourceReadyPollerObject(
                            regionOperations,
                            stack,
                            reservedIpCreateRequest.getAddress().getName(),
                            execute.getName(),
                            ResourceType.GCC_RESERVED_IP);
            gccReservedIpReadyPollerObjectPollingService.pollWithTimeout(gccResourceCheckerStatus, instReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        } else {
            throw new GcpResourceException(execute.getHttpErrorMessage(), resourceType(), reservedIpCreateRequest.getAddress().getName());
        }
        return true;
    }

    @Override
    public Boolean delete(Resource resource, GccDeleteContextObject deleteContextObject, String region) throws Exception {
        Stack stack = stackRepository.findById(deleteContextObject.getStackId());
        try {
            GccCredential gccCredential = (GccCredential) stack.getCredential();
            Operation operation = deleteContextObject
                    .getCompute()
                    .addresses()
                    .delete(gccCredential.getProjectId(), GccZone.valueOf(region).getRegion(), resource.getResourceName())
                    .execute();
            Compute.RegionOperations.Get regionOperations = createRegionOperations(
                    deleteContextObject.getCompute(),
                    gccCredential,
                    operation,
                    GccZone.valueOf(region)
            );
            Compute.GlobalOperations.Get globalOperations = createGlobalOperations(
                    deleteContextObject.getCompute(),
                    gccCredential,
                    operation
            );
            GccRemoveReadyPollerObject gccRemoveReady = new GccRemoveReadyPollerObject(
                    regionOperations,
                    globalOperations,
                    stack,
                    resource.getResourceName(),
                    operation.getName(),
                    resourceType()
            );
            gccRemoveReadyPollerObjectPollingService.pollWithTimeout(gccRemoveCheckerStatus, gccRemoveReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        } catch (GoogleJsonResponseException ex) {
            exceptionHandler(ex, resource.getResourceName(), stack);
        } catch (IOException e) {
            throw new InternalServerException(e.getMessage());
        }
        return true;
    }

    @Override
    public List<Resource> buildResources(GccProvisionContextObject provisionContextObject, int index, List<Resource> resources,
            Optional<InstanceGroup> instanceGroup) {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        return Arrays.asList(new Resource(resourceType(), stack.getName() + "reservedip", stack, null));
    }

    @Override
    public CreateResourceRequest buildCreateRequest(GccProvisionContextObject provisionContextObject, List<Resource> resources, List<Resource> buildResources,
            int index, Optional<InstanceGroup> instanceGroup, Optional<String> userData) throws Exception {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        Address address = new Address();
        address.setRegion(GccZone.valueOf(stack.getRegion()).getRegion());
        address.setName(buildResources.get(0).getResourceName());
        return new GccReservedIpCreateRequest(provisionContextObject.getStackId(), address,
                provisionContextObject.getProjectId(), provisionContextObject.getCompute(),
                GccZone.valueOf(stack.getRegion()), buildResources, (GccCredential) stack.getCredential());
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCC_RESERVED_IP;
    }

    public class GccReservedIpCreateRequest extends CreateResourceRequest {
        private Address address;
        private Long stackId;
        private String projectId;
        private Compute compute;
        private GccZone gccZone;
        private GccCredential gccCredential;

        public GccReservedIpCreateRequest(Long stackId, Address address, String projectId, Compute compute, GccZone gccZone, List<Resource> buildNames,
                GccCredential gccCredential) {
            super(buildNames);
            this.stackId = stackId;
            this.address = address;
            this.projectId = projectId;
            this.compute = compute;
            this.gccZone = gccZone;
            this.gccCredential = gccCredential;
        }

        public Long getStackId() {
            return stackId;
        }

        public Address getAddress() {
            return address;
        }

        public GccZone getGccZone() {
            return gccZone;
        }

        public String getProjectId() {
            return projectId;
        }

        public Compute getCompute() {
            return compute;
        }

        public GccCredential getGccCredential() {
            return gccCredential;
        }
    }
}
