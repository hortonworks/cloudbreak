package com.sequenceiq.cloudbreak.service.stack.resource.gcc.builders.network;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.ERROR;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Network;
import com.google.api.services.compute.model.Operation;
import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.domain.GccTemplate;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccRemoveCheckerStatus;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccRemoveReadyPollerObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.GccSimpleNetworkResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccDescribeContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccStartStopContextObject;

@Component
@Order(1)
public class GccNetworkResourceBuilder extends GccSimpleNetworkResourceBuilder {

    @Autowired
    private StackRepository stackRepository;
    @Autowired
    private GccRemoveCheckerStatus gccRemoveCheckerStatus;
    @Autowired
    private PollingService<GccRemoveReadyPollerObject> gccRemoveReadyPollerObjectPollingService;
    @Autowired
    private JsonHelper jsonHelper;

    @Override
    public List<Resource> create(GccProvisionContextObject contextObject, int index, List<Resource> resources) throws Exception {
        Stack stack = stackRepository.findById(contextObject.getStackId());
        Network network = new Network();
        network.setName(stack.getName());
        network.setIPv4Range("10.0.0.0/24");
        Compute.Networks.Insert networkInsert = contextObject.getCompute().networks().insert(contextObject.getProjectId(), network);
        networkInsert.execute();
        return Arrays.asList(new Resource(resourceType(), stack.getName(), stack));
    }

    @Override
    public Boolean delete(Resource resource, GccDeleteContextObject d) throws Exception {
        Stack stack = stackRepository.findById(d.getStackId());
        try {
            GccTemplate gccTemplate = (GccTemplate) stack.getTemplate();
            GccCredential gccCredential = (GccCredential) stack.getCredential();
            Operation execute = d.getCompute().networks().delete(gccCredential.getProjectId(), resource.getResourceName()).execute();
            Compute.ZoneOperations.Get zoneOperations = createZoneOperations(d.getCompute(), gccCredential, gccTemplate, execute);
            Compute.GlobalOperations.Get globalOperations = createGlobalOperations(d.getCompute(), gccCredential, gccTemplate, execute);
            GccRemoveReadyPollerObject gccRemoveReady =
                    new GccRemoveReadyPollerObject(zoneOperations, globalOperations, stack.getId(), resource.getResourceName());
            gccRemoveReadyPollerObjectPollingService.pollWithTimeout(gccRemoveCheckerStatus, gccRemoveReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        } catch (GoogleJsonResponseException ex) {
            exceptionHandler(ex, resource.getResourceName(), stack);
        } catch (IOException e) {
            throw new InternalServerException(e.getMessage());
        }
        return true;
    }

    @Override
    public Optional<String> describe(Resource resource, GccDescribeContextObject dco) throws Exception {
        Stack stack = stackRepository.findById(dco.getStackId());
        GccCredential gccCredential = (GccCredential) stack.getCredential();
        try {
            Compute.Networks.Get getNetwork = dco.getCompute().networks().get(gccCredential.getProjectId(), resource.getResourceName());
            return Optional.fromNullable(getNetwork.execute().toPrettyString());
        } catch (IOException e) {
            return Optional.fromNullable(jsonHelper.createJsonFromString(String.format("{\"Network\": {%s}}", ERROR)).toString());
        }
    }

    @Override
    public Boolean start(GccStartStopContextObject startStopContextObject, Resource resource) {
        return true;
    }

    @Override
    public Boolean stop(GccStartStopContextObject startStopContextObject, Resource resource) {
        return true;
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCC_NETWORK;
    }

}
