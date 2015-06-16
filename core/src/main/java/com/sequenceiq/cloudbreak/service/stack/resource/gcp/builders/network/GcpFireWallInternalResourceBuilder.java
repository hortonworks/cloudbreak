package com.sequenceiq.cloudbreak.service.stack.resource.gcp.builders.network;

import static com.sequenceiq.cloudbreak.domain.ResourceType.GCP_FIREWALL_INTERNAL;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Firewall;
import com.google.api.services.compute.model.Operation;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
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
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.GcpSimpleNetworkResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.model.GcpDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.model.GcpProvisionContextObject;

@Component
@Order(2)
public class GcpFireWallInternalResourceBuilder extends GcpSimpleNetworkResourceBuilder {

    @Inject
    private StackRepository stackRepository;
    @Inject
    private GcpRemoveCheckerStatus gcpRemoveCheckerStatus;
    @Inject
    private PollingService<GcpRemoveReadyPollerObject> gcpRemoveReadyPollerObjectPollingService;
    @Inject
    private PollingService<GcpResourceReadyPollerObject> gcpFirewallInternalReadyPollerObjectPollingService;
    @Inject
    private GcpResourceCheckerStatus gcpResourceCheckerStatus;

    @Override
    public Boolean create(CreateResourceRequest createResourceRequest, String region) throws Exception {
        final GcpFireWallOutCreateRequest gFWOCR = (GcpFireWallOutCreateRequest) createResourceRequest;
        Compute.Firewalls.Insert firewallInsert = gFWOCR.getCompute().firewalls().insert(gFWOCR.getProjectId(), gFWOCR.getFirewall());
        Operation execute = firewallInsert.execute();
        Stack stack = stackRepository.findById(gFWOCR.getStackId());
        if (execute.getHttpErrorStatusCode() == null) {
            Compute.GlobalOperations.Get globalOperations = createGlobalOperations(gFWOCR.getCompute(), (GcpCredential) stack.getCredential(), execute);
            GcpResourceReadyPollerObject instReady =
                    new GcpResourceReadyPollerObject(globalOperations, stack, gFWOCR.getFirewall().getName(), execute.getName(), GCP_FIREWALL_INTERNAL);
            gcpFirewallInternalReadyPollerObjectPollingService.pollWithTimeout(gcpResourceCheckerStatus, instReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        } else {
            throw new GcpResourceException(execute.getHttpErrorMessage(), resourceType(), gFWOCR.getFirewall().getName());
        }
        return true;
    }

    @Override
    public Boolean delete(Resource resource, GcpDeleteContextObject deleteContextObject, String region) throws Exception {
        Stack stack = stackRepository.findById(deleteContextObject.getStackId());
        try {
            GcpCredential gcpCredential = (GcpCredential) stack.getCredential();
            Operation operation = deleteContextObject.getCompute().firewalls().delete(gcpCredential.getProjectId(), resource.getResourceName()).execute();
            Compute.ZoneOperations.Get zoneOperations =
                    createZoneOperations(deleteContextObject.getCompute(), gcpCredential, operation, CloudRegion.valueOf(region));
            Compute.GlobalOperations.Get globalOperations = createGlobalOperations(deleteContextObject.getCompute(), gcpCredential, operation);
            GcpRemoveReadyPollerObject gcpRemoveReady =
                    new GcpRemoveReadyPollerObject(zoneOperations, globalOperations, stack, resource.getResourceName(), operation.getName(), resourceType());
            gcpRemoveReadyPollerObjectPollingService.pollWithTimeout(gcpRemoveCheckerStatus, gcpRemoveReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        } catch (GoogleJsonResponseException ex) {
            exceptionHandler(ex, resource.getResourceName(), stack);
        } catch (IOException e) {
            throw new GcpResourceException("Error during deletion!", e);
        }
        return true;
    }

    @Override
    public List<Resource> buildResources(GcpProvisionContextObject provisionContextObject, int index, List<Resource> resources,
            Optional<InstanceGroup> instanceGroup) {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        return Arrays.asList(new Resource(resourceType(), getTimestampedName(stack.getName() + "internal"), stack, null));
    }

    @Override
    public CreateResourceRequest buildCreateRequest(GcpProvisionContextObject provisionContextObject, List<Resource> resources,
            List<Resource> buildResources, int index, Optional<InstanceGroup> instanceGroup, Optional<String> userData) throws Exception {
        Firewall firewall = new Firewall();
        Firewall.Allowed allowed1 = new Firewall.Allowed();
        allowed1.setIPProtocol("tcp");
        allowed1.setPorts(ImmutableList.of("1-65535"));

        Firewall.Allowed allowed2 = new Firewall.Allowed();
        allowed2.setIPProtocol("icmp");

        Firewall.Allowed allowed3 = new Firewall.Allowed();
        allowed3.setIPProtocol("udp");
        allowed3.setPorts(ImmutableList.of("1-65535"));

        firewall.setAllowed(ImmutableList.of(allowed1, allowed2, allowed3));
        firewall.setName(buildResources.get(0).getResourceName());
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        firewall.setSourceRanges(ImmutableList.of(stack.getNetwork().getSubnetCIDR()));
        firewall.setNetwork(String.format("https://www.googleapis.com/compute/v1/projects/%s/global/networks/%s",
                provisionContextObject.getProjectId(), provisionContextObject.filterResourcesByType(ResourceType.GCP_NETWORK).get(0).getResourceName()));
        return new GcpFireWallOutCreateRequest(provisionContextObject.getStackId(), firewall, provisionContextObject.getProjectId(),
                provisionContextObject.getCompute(), buildResources);
    }

    @Override
    public ResourceType resourceType() {
        return GCP_FIREWALL_INTERNAL;
    }

    public class GcpFireWallOutCreateRequest extends CreateResourceRequest {
        private Long stackId;
        private Firewall firewall;
        private String projectId;
        private Compute compute;

        public GcpFireWallOutCreateRequest(Long stackId, Firewall firewall, String projectId, Compute compute, List<Resource> buildNames) {
            super(buildNames);
            this.stackId = stackId;
            this.firewall = firewall;
            this.projectId = projectId;
            this.compute = compute;
        }

        public Long getStackId() {
            return stackId;
        }

        public Firewall getFirewall() {
            return firewall;
        }

        public String getProjectId() {
            return projectId;
        }

        public Compute getCompute() {
            return compute;
        }
    }

}
