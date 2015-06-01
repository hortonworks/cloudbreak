package com.sequenceiq.cloudbreak.service.stack.resource.gcp.builders.network;

import static com.sequenceiq.cloudbreak.domain.ResourceType.GCP_FIREWALL_IN;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Firewall;
import com.google.api.services.compute.model.Operation;
import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.domain.GcpCredential;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Subnet;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.network.NetworkUtils;
import com.sequenceiq.cloudbreak.service.stack.connector.gcp.GcpRemoveCheckerStatus;
import com.sequenceiq.cloudbreak.service.stack.connector.gcp.GcpRemoveReadyPollerObject;
import com.sequenceiq.cloudbreak.service.stack.connector.gcp.GcpResourceCheckerStatus;
import com.sequenceiq.cloudbreak.service.stack.connector.gcp.GcpResourceReadyPollerObject;
import com.sequenceiq.cloudbreak.service.stack.connector.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.domain.GcpZone;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.GcpSimpleNetworkResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.model.GcpDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.model.GcpProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.model.GcpUpdateContextObject;

@Component
@Order(3)
public class GcpFireWallInResourceBuilder extends GcpSimpleNetworkResourceBuilder {

    @Autowired
    private StackRepository stackRepository;
    @Autowired
    private GcpRemoveCheckerStatus gcpRemoveCheckerStatus;
    @Autowired
    private PollingService<GcpRemoveReadyPollerObject> gcpRemoveReadyPollerObjectPollingService;
    @Autowired
    private PollingService<GcpResourceReadyPollerObject> gcpFirewallInternalReadyPollerObjectPollingService;
    @Autowired
    private GcpResourceCheckerStatus gcpResourceCheckerStatus;

    @Override
    public Boolean create(CreateResourceRequest createResourceRequest, String region) throws Exception {
        final GcpFireWallInCreateRequest gFWICR = (GcpFireWallInCreateRequest) createResourceRequest;
        Compute.Firewalls.Insert firewallInsert = gFWICR.getCompute().firewalls().insert(gFWICR.getProjectId(), gFWICR.getFirewall());
        Operation execute = firewallInsert.execute();
        Stack stack = stackRepository.findById(gFWICR.getStackId());
        if (execute.getHttpErrorStatusCode() == null) {
            Compute.GlobalOperations.Get globalOperations = createGlobalOperations(gFWICR.getCompute(), (GcpCredential) stack.getCredential(), execute);
            GcpResourceReadyPollerObject instReady =
                    new GcpResourceReadyPollerObject(globalOperations, stack, gFWICR.getFirewall().getName(), execute.getName(), GCP_FIREWALL_IN);
            gcpFirewallInternalReadyPollerObjectPollingService.pollWithTimeout(gcpResourceCheckerStatus, instReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        } else {
            throw new GcpResourceException(execute.getHttpErrorMessage(), resourceType(), gFWICR.getFirewall().getName());
        }
        return true;
    }

    @Override
    public Boolean delete(Resource resource, GcpDeleteContextObject deleteContextObject, String region) throws Exception {
        Stack stack = stackRepository.findById(deleteContextObject.getStackId());
        try {
            GcpCredential gcpCredential = (GcpCredential) stack.getCredential();
            Operation operation = deleteContextObject.getCompute().firewalls().delete(gcpCredential.getProjectId(), resource.getResourceName()).execute();
            Compute.ZoneOperations.Get zoneOperations = createZoneOperations(deleteContextObject.getCompute(),
                    gcpCredential, operation, GcpZone.valueOf(region));
            Compute.GlobalOperations.Get globalOperations = createGlobalOperations(deleteContextObject.getCompute(), gcpCredential, operation);
            GcpRemoveReadyPollerObject gcpRemoveReady =
                    new GcpRemoveReadyPollerObject(zoneOperations, globalOperations, stack, resource.getResourceName(), operation.getName(), resourceType());
            gcpRemoveReadyPollerObjectPollingService.pollWithTimeout(gcpRemoveCheckerStatus, gcpRemoveReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        } catch (GoogleJsonResponseException ex) {
            exceptionHandler(ex, resource.getResourceName(), stack);
        } catch (IOException e) {
            throw new GcpResourceException("Error during deletion", e);
        }
        return true;
    }

    @Override
    public List<Resource> buildResources(GcpProvisionContextObject provisionContextObject, int index, List<Resource> resources,
            Optional<InstanceGroup> instanceGroup) {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        return Arrays.asList(new Resource(resourceType(), getTimestampedName(stack.getName() + "in"), stack, null));
    }

    @Override
    public CreateResourceRequest buildCreateRequest(GcpProvisionContextObject provisionContextObject, List<Resource> resources,
            List<Resource> buildResources, int index, Optional<InstanceGroup> instanceGroup, Optional<String> userData) throws Exception {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());

        List<String> sourceRanges = getSourceRanges(stack);

        Firewall firewall = new Firewall();
        firewall.setSourceRanges(sourceRanges);

        List<Firewall.Allowed> allowedRules = new ArrayList<>();
        allowedRules.add(new Firewall.Allowed().setIPProtocol("icmp"));

        Firewall.Allowed tcp = createRule(stack, "tcp");
        if (tcp != null) {
            allowedRules.add(tcp);
        }

        Firewall.Allowed udp = createRule(stack, "udp");
        if (udp != null) {
            allowedRules.add(udp);
        }

        firewall.setAllowed(allowedRules);
        firewall.setName(buildResources.get(0).getResourceName());
        firewall.setNetwork(String.format("https://www.googleapis.com/compute/v1/projects/%s/global/networks/%s",
                provisionContextObject.getProjectId(), provisionContextObject.filterResourcesByType(ResourceType.GCP_NETWORK).get(0).getResourceName()));
        return new GcpFireWallInCreateRequest(provisionContextObject.getStackId(), firewall, provisionContextObject.getProjectId(),
                provisionContextObject.getCompute(), buildResources);
    }

    @Override
    public void update(GcpUpdateContextObject updateContextObject) {
        Stack stack = updateContextObject.getStack();
        Compute compute = updateContextObject.getCompute();
        String project = updateContextObject.getProject();
        String resourceName = stack.getResourceByType(GCP_FIREWALL_IN).getResourceName();
        try {
            Firewall fireWall = compute.firewalls().get(project, resourceName).execute();
            List<String> sourceRanges = getSourceRanges(stack);
            fireWall.setSourceRanges(sourceRanges);
            compute.firewalls().update(project, resourceName, fireWall).execute();
        } catch (IOException e) {
            throw new GcpResourceException("Failed to update resource!", GCP_FIREWALL_IN, resourceName, e);
        }
    }

    @Override
    public ResourceType resourceType() {
        return GCP_FIREWALL_IN;
    }

    private List<String> getSourceRanges(Stack stack) {
        List<String> sourceRanges = new ArrayList<>(stack.getAllowedSubnets().size());
        for (Subnet subnet : stack.getAllowedSubnets()) {
            sourceRanges.add(subnet.getCidr());
        }
        return sourceRanges;
    }

    private Firewall.Allowed createRule(Stack stack, String protocol) {
        List<String> ports = NetworkUtils.getRawPorts(stack, protocol);
        if (!ports.isEmpty()) {
            Firewall.Allowed rule = new Firewall.Allowed();
            rule.setIPProtocol(protocol);
            rule.setPorts(ports);
            return rule;
        }
        return null;
    }

    public class GcpFireWallInCreateRequest extends CreateResourceRequest {
        private Long stackId;
        private Firewall firewall;
        private String projectId;
        private Compute compute;

        public GcpFireWallInCreateRequest(Long stackId, Firewall firewall, String projectId, Compute compute, List<Resource> buildNames) {
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
