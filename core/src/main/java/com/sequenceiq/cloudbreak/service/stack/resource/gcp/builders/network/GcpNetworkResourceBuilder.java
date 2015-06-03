package com.sequenceiq.cloudbreak.service.stack.resource.gcp.builders.network;

import static com.sequenceiq.cloudbreak.domain.ResourceType.GCP_NETWORK;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Network;
import com.google.api.services.compute.model.Operation;
import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.domain.GcpCredential;
import com.sequenceiq.cloudbreak.domain.GcpZone;
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
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.GcpSimpleNetworkResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.model.GcpDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.model.GcpProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.model.GcpStartStopContextObject;

@Component
@Order(1)
public class GcpNetworkResourceBuilder extends GcpSimpleNetworkResourceBuilder {

    @Inject
    private StackRepository stackRepository;
    @Inject
    private GcpRemoveCheckerStatus gcpRemoveCheckerStatus;
    @Inject
    private PollingService<GcpRemoveReadyPollerObject> gcpRemoveReadyPollerObjectPollingService;
    @Inject
    private PollingService<GcpResourceReadyPollerObject> gcpNetworkReadyPollerObjectPollingService;
    @Inject
    private GcpResourceCheckerStatus gcpResourceCheckerStatus;

    @Override
    public Boolean create(CreateResourceRequest createResourceRequest, String region) throws Exception {
        final GcpNetworkCreateRequest gNCR = (GcpNetworkCreateRequest) createResourceRequest;
        Stack stack = stackRepository.findById(gNCR.getStackId());
        Compute.Networks.Insert networkInsert = gNCR.getCompute().networks().insert(gNCR.getProjectId(), gNCR.getNetwork());
        Operation execute = networkInsert.execute();
        if (execute.getHttpErrorStatusCode() == null) {
            Compute.GlobalOperations.Get globalOperations = createGlobalOperations(gNCR.getCompute(), (GcpCredential) stack.getCredential(), execute);
            GcpResourceReadyPollerObject instReady =
                    new GcpResourceReadyPollerObject(globalOperations, stack, gNCR.getNetwork().getName(), execute.getName(), GCP_NETWORK);
            gcpNetworkReadyPollerObjectPollingService.pollWithTimeout(gcpResourceCheckerStatus, instReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        } else {
            throw new GcpResourceException(execute.getHttpErrorMessage(), resourceType(), gNCR.getNetwork().getName());
        }
        return true;
    }

    @Override
    public Boolean delete(Resource resource, GcpDeleteContextObject deleteContextObject, String region) throws Exception {
        Stack stack = stackRepository.findById(deleteContextObject.getStackId());
        try {
            GcpCredential gcpCredential = (GcpCredential) stack.getCredential();
            Operation execute = deleteContextObject.getCompute().networks().delete(gcpCredential.getProjectId(), resource.getResourceName()).execute();
            Compute.ZoneOperations.Get zoneOperations = createZoneOperations(deleteContextObject.getCompute(), gcpCredential, execute, GcpZone.valueOf(region));
            Compute.GlobalOperations.Get globalOperations = createGlobalOperations(deleteContextObject.getCompute(), gcpCredential, execute);
            GcpRemoveReadyPollerObject gcpRemoveReady =
                    new GcpRemoveReadyPollerObject(zoneOperations, globalOperations, stack, resource.getResourceName(), execute.getName(), resourceType());
            gcpRemoveReadyPollerObjectPollingService.pollWithTimeout(gcpRemoveCheckerStatus, gcpRemoveReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        } catch (GoogleJsonResponseException ex) {
            exceptionHandler(ex, resource.getResourceName(), stack);
        } catch (IOException e) {
            throw new GcpResourceException("Error while deleting network resource", e);
        }
        return true;
    }

    @Override
    public Boolean start(GcpStartStopContextObject startStopContextObject, Resource resource, String region) {
        return true;
    }

    @Override
    public Boolean stop(GcpStartStopContextObject startStopContextObject, Resource resource, String region) {
        return true;
    }

    @Override
    public List<Resource> buildResources(GcpProvisionContextObject provisionContextObject, int index, List<Resource> resources,
            Optional<InstanceGroup> instanceGroup) {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        return Arrays.asList(new Resource(resourceType(), getTimestampedName(stack.getName()), stack, null));
    }

    @Override
    public CreateResourceRequest buildCreateRequest(GcpProvisionContextObject provisionContextObject, List<Resource> resources,
            List<Resource> buildResources, int index, Optional<InstanceGroup> instanceGroup, Optional<String> userData) throws Exception {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        Network network = new Network();
        network.setName(buildResources.get(0).getResourceName());
        network.setIPv4Range(stack.getNetwork().getSubnetCIDR());
        return new GcpNetworkCreateRequest(provisionContextObject.getStackId(), network, provisionContextObject.getProjectId(),
                provisionContextObject.getCompute(), buildResources);
    }

    @Override
    public ResourceType resourceType() {
        return GCP_NETWORK;
    }

    public class GcpNetworkCreateRequest extends CreateResourceRequest {
        private Long stackId;
        private Network network;
        private String projectId;
        private Compute compute;

        public GcpNetworkCreateRequest(Long stackId, Network network, String projectId, Compute compute, List<Resource> buildNames) {
            super(buildNames);
            this.stackId = stackId;
            this.network = network;
            this.projectId = projectId;
            this.compute = compute;
        }

        public Long getStackId() {
            return stackId;
        }

        public Network getNetwork() {
            return network;
        }

        public String getProjectId() {
            return projectId;
        }

        public Compute getCompute() {
            return compute;
        }
    }

}
