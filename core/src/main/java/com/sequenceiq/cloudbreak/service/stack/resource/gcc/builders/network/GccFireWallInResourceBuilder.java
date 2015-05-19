package com.sequenceiq.cloudbreak.service.stack.resource.gcc.builders.network;

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
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Subnet;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.network.NetworkUtils;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccRemoveCheckerStatus;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccRemoveReadyPollerObject;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GcpResourceException;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccZone;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.GccSimpleNetworkResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccUpdateContextObject;

@Component
@Order(3)
public class GccFireWallInResourceBuilder extends GccSimpleNetworkResourceBuilder {

    @Autowired
    private StackRepository stackRepository;
    @Autowired
    private GccRemoveCheckerStatus gccRemoveCheckerStatus;
    @Autowired
    private PollingService<GccRemoveReadyPollerObject> gccRemoveReadyPollerObjectPollingService;

    @Override
    public Boolean create(CreateResourceRequest createResourceRequest, String region) throws Exception {
        final GccFireWallInCreateRequest gFWICR = (GccFireWallInCreateRequest) createResourceRequest;
        Compute.Firewalls.Insert firewallInsert = gFWICR.getCompute().firewalls().insert(gFWICR.getProjectId(), gFWICR.getFirewall());
        firewallInsert.execute();
        return true;
    }

    @Override
    public Boolean delete(Resource resource, GccDeleteContextObject deleteContextObject, String region) throws Exception {
        Stack stack = stackRepository.findById(deleteContextObject.getStackId());
        try {
            GccCredential gccCredential = (GccCredential) stack.getCredential();
            Operation operation = deleteContextObject.getCompute().firewalls().delete(gccCredential.getProjectId(), resource.getResourceName()).execute();
            Compute.ZoneOperations.Get zoneOperations = createZoneOperations(deleteContextObject.getCompute(),
                    gccCredential, operation, GccZone.valueOf(region));
            Compute.GlobalOperations.Get globalOperations = createGlobalOperations(deleteContextObject.getCompute(), gccCredential, operation);
            GccRemoveReadyPollerObject gccRemoveReady =
                    new GccRemoveReadyPollerObject(zoneOperations, globalOperations, stack, resource.getResourceName(), operation.getName(), resourceType());
            gccRemoveReadyPollerObjectPollingService.pollWithTimeout(gccRemoveCheckerStatus, gccRemoveReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        } catch (GoogleJsonResponseException ex) {
            exceptionHandler(ex, resource.getResourceName(), stack);
        } catch (IOException e) {
            throw new GcpResourceException("Error during deletion", e);
        }
        return true;
    }

    @Override
    public List<Resource> buildResources(GccProvisionContextObject provisionContextObject, int index, List<Resource> resources,
            Optional<InstanceGroup> instanceGroup) {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        return Arrays.asList(new Resource(resourceType(), stack.getName() + "in", stack, null));
    }

    @Override
    public CreateResourceRequest buildCreateRequest(GccProvisionContextObject provisionContextObject, List<Resource> resources,
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
                provisionContextObject.getProjectId(), provisionContextObject.filterResourcesByType(ResourceType.GCC_NETWORK).get(0).getResourceName()));
        return new GccFireWallInCreateRequest(provisionContextObject.getStackId(), firewall, provisionContextObject.getProjectId(),
                provisionContextObject.getCompute(), buildResources);
    }

    @Override
    public void update(GccUpdateContextObject updateContextObject) {
        Stack stack = updateContextObject.getStack();
        Compute compute = updateContextObject.getCompute();
        String project = updateContextObject.getProject();
        String resourceName = stack.getResourceByType(ResourceType.GCC_FIREWALL_IN).getResourceName();
        try {
            Firewall fireWall = compute.firewalls().get(project, resourceName).execute();
            List<String> sourceRanges = getSourceRanges(stack);
            fireWall.setSourceRanges(sourceRanges);
            compute.firewalls().update(project, resourceName, fireWall).execute();
        } catch (IOException e) {
            throw new GcpResourceException("Failed to update resource!", ResourceType.GCC_FIREWALL_IN, resourceName, e);
        }
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCC_FIREWALL_IN;
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

    public class GccFireWallInCreateRequest extends CreateResourceRequest {
        private Long stackId;
        private Firewall firewall;
        private String projectId;
        private Compute compute;

        public GccFireWallInCreateRequest(Long stackId, Firewall firewall, String projectId, Compute compute, List<Resource> buildNames) {
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
