package com.sequenceiq.cloudbreak.service.stack.resource.gcc.builders.network;

import java.io.IOException;
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
import com.google.common.collect.ImmutableList;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.domain.GccTemplate;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.StackDependentPollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccRemoveCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccRemoveReadyPollerObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.GccSimpleNetworkResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccDescribeContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccProvisionContextObject;

@Component
@Order(3)
public class GccFireWallInResourceBuilder extends GccSimpleNetworkResourceBuilder {

    @Autowired
    private StackRepository stackRepository;
    @Autowired
    private GccRemoveCheckerTask gccRemoveCheckerTask;
    @Autowired
    private StackDependentPollingService<GccRemoveReadyPollerObject> gccRemoveReadyPollerObjectPollingService;
    @Autowired
    private JsonHelper jsonHelper;

    @Override
    public List<Resource> create(GccProvisionContextObject pO, int index, List<Resource> resources) throws Exception {
        Stack stack = stackRepository.findById(pO.getStackId());

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
        firewall.setName(stack.getName() + "in");
        firewall.setSourceRanges(ImmutableList.of("10.0.0.0/16"));
        firewall.setNetwork(String.format("https://www.googleapis.com/compute/v1/projects/%s/global/networks/%s",
                pO.getProjectId(), pO.filterResourcesByType(ResourceType.GCC_NETWORK).get(0).getResourceName()));
        Compute.Firewalls.Insert firewallInsert = pO.getCompute().firewalls().insert(pO.getProjectId(), firewall);
        firewallInsert.execute();
        return Arrays.asList(new Resource(resourceType(), stack.getName() + "in", stack));
    }

    @Override
    public Boolean delete(Resource resource, GccDeleteContextObject d) throws Exception {
        Stack stack = stackRepository.findById(d.getStackId());
        try {
            GccTemplate gccTemplate = (GccTemplate) stack.getTemplate();
            GccCredential gccCredential = (GccCredential) stack.getCredential();
            Operation operation = d.getCompute().firewalls().delete(gccCredential.getProjectId(), resource.getResourceName()).execute();
            Compute.ZoneOperations.Get zoneOperations = createZoneOperations(d.getCompute(), gccCredential, gccTemplate, operation);
            Compute.GlobalOperations.Get globalOperations = createGlobalOperations(d.getCompute(), gccCredential, gccTemplate, operation);
            GccRemoveReadyPollerObject gccRemoveReady =
                    new GccRemoveReadyPollerObject(zoneOperations, globalOperations, stack, resource.getResourceName(), operation.getName());
            gccRemoveReadyPollerObjectPollingService.pollWithTimeout(gccRemoveCheckerTask, gccRemoveReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        } catch (GoogleJsonResponseException ex) {
            exceptionHandler(ex, resource.getResourceName(), stack);
        } catch (IOException e) {
            throw new InternalServerException(e.getMessage());
        }
        return true;
    }

    @Override
    public Optional<String> describe(Resource resource, GccDescribeContextObject dco) throws Exception {
        return Optional.absent();
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCC_FIREWALL_IN;
    }

}
